import express from 'express';
import { OAuth2Client } from 'google-auth-library';
import { google } from 'googleapis';
import PDFDocument from 'pdfkit';
import { Readable } from 'node:stream';
import { randomUUID } from 'node:crypto';

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '256kb' }));

const config = {
  spreadsheetId: process.env.SHOP_SPREADSHEET_ID,
  invoiceFolderId: process.env.INVOICE_FOLDER_ID,
  oauthClientId: process.env.GOOGLE_OAUTH_CLIENT_ID,
  authorizedEmails: new Set((process.env.AUTHORIZED_EMAILS ?? '').split(',').map(v => v.trim().toLowerCase()).filter(Boolean)),
};

const idTokenVerifier = new OAuth2Client();
const requiredHeaders = {
  Inventory: ['SKU', 'Product Name', 'Category', 'Supplier', 'Unit Cost', 'Selling Price', 'Opening Stock', 'Reorder Level', 'Purchased Qty', 'Sold Qty', 'Current Stock', 'Stock Value', 'Stock Status', 'QR Code'],
  Customers: ['Customer ID', 'Customer Name', 'Phone', 'Email', 'City / Address', 'Customer Type', 'Total Orders', 'Total Spent', 'Last Purchase Date'],
  'Sales Invoices': ['Invoice #', 'Date', 'Customer ID', 'Customer Name', 'Items', 'Invoice Total', 'Gross Profit', 'Payment Mode', 'Invoice File', 'Customer Phone'],
  'Sales Items': ['Invoice #', 'Date', 'SKU', 'Product Name', 'Category', 'Quantity', 'Unit Price', 'Discount', 'Line Total', 'COGS', 'Gross Profit'],
};
const primaryKey = { Inventory: 'SKU', Customers: 'Customer ID', 'Sales Invoices': 'Invoice #', 'Sales Items': 'Invoice #' };

let clientsPromise;
async function clients() {
  clientsPromise ??= (async () => {
    const auth = new google.auth.GoogleAuth({
      scopes: [
        'https://www.googleapis.com/auth/spreadsheets',
        'https://www.googleapis.com/auth/drive',
      ],
    });
    const authClient = await auth.getClient();
    return {
      sheets: google.sheets({ version: 'v4', auth: authClient }),
      drive: google.drive({ version: 'v3', auth: authClient }),
    };
  })();
  return clientsPromise;
}

function requireConfig() {
  if (!config.spreadsheetId || !config.invoiceFolderId || !config.oauthClientId || config.authorizedEmails.size === 0) {
    const error = new Error('The service is not configured.');
    error.status = 503;
    throw error;
  }
}

async function authenticate(req, res, next) {
  try {
    requireConfig();
    const token = req.headers.authorization?.match(/^Bearer\s+(.+)$/i)?.[1];
    if (!token) return res.status(401).json({ error: 'Sign in to use Sales App.' });
    const ticket = await idTokenVerifier.verifyIdToken({ idToken: token, audience: config.oauthClientId });
    const payload = ticket.getPayload();
    const email = payload?.email?.toLowerCase();
    if (!email || payload.email_verified !== true || !config.authorizedEmails.has(email)) {
      return res.status(403).json({ error: 'This Google account is not allowed to use Sales App.' });
    }
    req.userEmail = email;
    next();
  } catch (error) {
    next(error);
  }
}

const sheetsCache = new Map();
async function getTable(title) {
  if (!requiredHeaders[title]) throw new Error(`Unsupported sheet tab: ${title}`);
  const { sheets } = await clients();
  const { data: metadata } = await sheets.spreadsheets.get({
    spreadsheetId: config.spreadsheetId,
    fields: 'sheets(properties(sheetId,title))',
  });
  const properties = metadata.sheets?.map(s => s.properties).find(s => s.title === title);
  if (!properties) throw new Error(`Required tab '${title}' is missing from Shop.`);

  const { data } = await sheets.spreadsheets.values.get({
    spreadsheetId: config.spreadsheetId,
    range: `'${title.replaceAll("'", "''")}'!A1:Z1000`,
    valueRenderOption: 'UNFORMATTED_VALUE',
  });
  const [headers = [], ...rows] = data.values ?? [];
  const missing = requiredHeaders[title].filter(header => !headers.includes(header));
  if (missing.length) throw new Error(`Tab '${title}' is missing columns: ${missing.join(', ')}`);
  const records = rows.map((row, index) => ({
    _rowNumber: index + 2,
    ...Object.fromEntries(headers.map((header, column) => [header, row[column] ?? ''])),
  }));
  const actualRows = records.filter(record => String(record[primaryKey[title]] ?? '').trim() !== '');
  return { sheetId: properties.sheetId, headers, rows: actualRows, allRows: records };
}

function normalizedPhone(value) {
  const digits = String(value ?? '').replace(/\D/g, '');
  if (digits.length === 12 && digits.startsWith('91')) return digits.slice(-10);
  return digits;
}

function nextFreeRow(table) {
  const occupied = new Set(table.rows.map(row => row._rowNumber));
  for (let rowNumber = 2; rowNumber <= 1000; rowNumber += 1) {
    if (!occupied.has(rowNumber)) return rowNumber;
  }
  const error = new Error(`Tab has reached its current 1,000-row limit.`);
  error.status = 409;
  throw error;
}

function cellValue(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return { numberValue: value };
  if (typeof value === 'boolean') return { boolValue: value };
  return { stringValue: String(value ?? '') };
}

function updateRowRequest(sheetId, rowNumber, headers, record, formulas = {}) {
  const values = headers.map(header => ({ userEnteredValue: { stringValue: '' } }));
  for (const [header, value] of Object.entries(record)) {
    const index = headers.indexOf(header);
    if (index >= 0) values[index] = { userEnteredValue: cellValue(value) };
  }
  for (const [header, formula] of Object.entries(formulas)) {
    const index = headers.indexOf(header);
    if (index >= 0) values[index] = { userEnteredValue: { formulaValue: formula } };
  }
  return {
    updateCells: {
      range: { sheetId, startRowIndex: rowNumber - 1, endRowIndex: rowNumber, startColumnIndex: 0, endColumnIndex: headers.length },
      rows: [{ values }],
      fields: 'userEnteredValue',
    },
  };
}

function formulaColumnRequest(sheetId, startRow, endRow, columnIndex, formulas) {
  if (endRow < startRow) return null;
  return {
    updateCells: {
      range: { sheetId, startRowIndex: startRow - 1, endRowIndex: endRow, startColumnIndex: columnIndex, endColumnIndex: columnIndex + 1 },
      rows: formulas.map(formula => ({ values: [{ userEnteredValue: { formulaValue: formula } }] })),
      fields: 'userEnteredValue',
    },
  };
}

function formulasForRow(title, row) {
  if (title === 'Inventory') return {
    'Purchased Qty': `=IF(A${row}="","",SUMIFS('Purchase Items'!$F$2:$F$1000,'Purchase Items'!$C$2:$C$1000,A${row}))`,
    'Sold Qty': `=IF(A${row}="","",SUMIFS('Sales Items'!$F$2:$F$1000,'Sales Items'!$C$2:$C$1000,A${row}))`,
    'Current Stock': `=IF(A${row}="","",G${row}+I${row}-J${row})`,
    'Stock Value': `=IF(A${row}="","",K${row}*E${row})`,
    'Stock Status': `=IF(A${row}="","",IF(K${row}<=0,"Out of Stock",IF(K${row}<=H${row},"Low Stock","OK")))`,
  };
  if (title === 'Customers') return {
    'Total Orders': `=IF(A${row}="","",COUNTIF('Sales Invoices'!$C$2:$C$1000,A${row}))`,
    'Total Spent': `=IF(A${row}="","",SUMIF('Sales Invoices'!$C$2:$C$1000,A${row},'Sales Invoices'!$F$2:$F$1000))`,
    'Last Purchase Date': `=IF(A${row}="","",IF(COUNTIF('Sales Invoices'!$C$2:$C$1000,A${row})=0,"",MAXIFS('Sales Invoices'!$B$2:$B$1000,'Sales Invoices'!$C$2:$C$1000,A${row})))`,
  };
  if (title === 'Sales Invoices') return {
    'Customer Name': `=IF(C${row}="","",IFERROR(VLOOKUP(C${row},Customers!$A$2:$I$1000,2,FALSE),""))`,
    Items: `=IF(A${row}="","",COUNTIF('Sales Items'!$A$2:$A$1000,A${row}))`,
    'Invoice Total': `=IF(A${row}="","",SUMIF('Sales Items'!$A$2:$A$1000,A${row},'Sales Items'!$I$2:$I$1000))`,
    'Gross Profit': `=IF(A${row}="","",SUMIF('Sales Items'!$A$2:$A$1000,A${row},'Sales Items'!$K$2:$K$1000))`,
  };
  if (title === 'Sales Items') return {
    Date: `=IF(A${row}="","",IFERROR(VLOOKUP(A${row},'Sales Invoices'!$A$2:$H$1000,2,FALSE),""))`,
    'Product Name': `=IF(C${row}="","",IFERROR(VLOOKUP(C${row},Inventory!$A$2:$M$1000,2,FALSE),""))`,
    Category: `=IF(C${row}="","",IFERROR(VLOOKUP(C${row},Inventory!$A$2:$M$1000,3,FALSE),""))`,
    'Unit Price': `=IF(C${row}="","",IFERROR(VLOOKUP(C${row},Inventory!$A$2:$M$1000,6,FALSE),""))`,
    'Line Total': `=IF(OR(C${row}="",F${row}=""),"",MAX(0,F${row}*G${row}-H${row}))`,
    COGS: `=IF(OR(C${row}="",F${row}=""),"",F${row}*IFERROR(VLOOKUP(C${row},Inventory!$A$2:$M$1000,5,FALSE),0))`,
    'Gross Profit': `=IF(OR(C${row}="",F${row}=""),"",I${row}-J${row})`,
  };
  return {};
}

async function writeRecords(rowPlans, affectedTables) {
  const tables = new Map();
  for (const title of new Set([...affectedTables, ...rowPlans.map(plan => plan.title)])) tables.set(title, await getTable(title));
  const requests = [];
  for (const plan of rowPlans) {
    requests.push(updateRowRequest(plan.table.sheetId, plan.rowNumber, plan.table.headers, plan.record, plan.formulas));
  }
  for (const title of affectedTables) {
    const table = tables.get(title);
    const plannedRows = rowPlans.filter(plan => plan.title === title).map(plan => plan.rowNumber);
    const lastRow = Math.max(1, ...table.rows.map(row => row._rowNumber), ...plannedRows);
    const formulaSamples = formulasForRow(title, 2);
    for (const header of Object.keys(formulaSamples)) {
      const columnIndex = table.headers.indexOf(header);
      const formulas = Array.from({ length: Math.max(0, lastRow - 1) }, (_, index) => formulasForRow(title, index + 2)[header]);
      const request = formulaColumnRequest(table.sheetId, 2, lastRow, columnIndex, formulas);
      if (request) requests.push(request);
    }
    if (title === 'Inventory') {
      const skuIndex = table.headers.indexOf('SKU');
      const nameIndex = table.headers.indexOf('Product Name');
      const priceIndex = table.headers.indexOf('Selling Price');
      const qrIndex = table.headers.indexOf('QR Code');
      if (skuIndex >= 0 && nameIndex >= 0 && priceIndex >= 0 && qrIndex >= 0) {
        const qrFormula = '=MAP(A2:A1000,LAMBDA(sku,IF(sku="","",IMAGE("https://quickchart.io/qr?text="&ENCODEURL(sku)&"&size=150&margin=1))))';
        requests.push({ updateCells: { range: { sheetId: table.sheetId, startRowIndex: 1, endRowIndex: 2, startColumnIndex: qrIndex, endColumnIndex: qrIndex + 1 }, rows: [{ values: [{ userEnteredValue: { formulaValue: qrFormula } }] }], fields: 'userEnteredValue' } });
      }
    }
  }
  const { sheets } = await clients();
  await sheets.spreadsheets.batchUpdate({ spreadsheetId: config.spreadsheetId, requestBody: { requests } });
}

async function findCustomer(phone) {
  const table = await getTable('Customers');
  const digits = normalizedPhone(phone);
  return table.rows.find(row => normalizedPhone(row.Phone) === digits) ?? null;
}

function newId(prefix) { return `${prefix}-${randomUUID().replaceAll('-', '').slice(0, 10).toUpperCase()}`; }
function invoiceNumber() {
  const date = new Date().toISOString().slice(0, 10).replaceAll('-', '');
  return `INV-${date}-${randomUUID().slice(0, 6).toUpperCase()}`;
}

function sheetDateSerial(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return (Date.UTC(year, month - 1, day) - Date.UTC(1899, 11, 30)) / 86_400_000;
}

function buildInvoicePdf(invoice, customer, lines) {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 44, bufferPages: true });
    const chunks = [];
    doc.on('data', chunk => chunks.push(chunk));
    doc.on('error', reject);
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.fillColor('#153447').fontSize(24).font('Helvetica-Bold').text('SALES INVOICE', { align: 'center' });
    doc.moveDown(0.4).fontSize(12).font('Helvetica').fillColor('#5B6E75').text(invoice.number, { align: 'center' });
    doc.moveDown(1.6).fillColor('#153447').font('Helvetica-Bold').text('Customer');
    doc.font('Helvetica').text(customer.name || 'Walk-in customer');
    doc.text(customer.phone);
    doc.moveDown(0.6).text(`Date: ${invoice.date}`);
    doc.text(`Payment: ${invoice.paymentMode}`);
    doc.moveDown(1.2);
    const x = [44, 288, 350, 418, 505];
    doc.font('Helvetica-Bold').fillColor('#153447').text('Item', x[0], doc.y, { width: 236 });
    doc.text('Qty', x[1], doc.y - 14, { width: 50, align: 'right' });
    doc.text('Price', x[2], doc.y - 14, { width: 62, align: 'right' });
    doc.text('Total', x[3], doc.y - 14, { width: 90, align: 'right' });
    doc.moveDown(0.7).strokeColor('#D7E2E1').moveTo(44, doc.y).lineTo(551, doc.y).stroke().moveDown(0.6);
    for (const line of lines) {
      const y = doc.y;
      doc.font('Helvetica').fillColor('#153447').fontSize(10).text(line.name, x[0], y, { width: 232 });
      doc.text(String(line.quantity), x[1], y, { width: 50, align: 'right' });
      doc.text(`Rs ${line.unitPrice.toFixed(2)}`, x[2], y, { width: 62, align: 'right' });
      doc.text(`Rs ${line.lineTotal.toFixed(2)}`, x[3], y, { width: 90, align: 'right' });
      doc.moveDown(0.8);
    }
    doc.moveDown(0.6).strokeColor('#D7E2E1').moveTo(320, doc.y).lineTo(551, doc.y).stroke().moveDown(0.9);
    doc.font('Helvetica-Bold').fontSize(16).fillColor('#147D78').text(`TOTAL  Rs ${invoice.total.toFixed(2)}`, 320, doc.y, { width: 231, align: 'right' });
    doc.moveDown(2).font('Helvetica').fontSize(11).fillColor('#5B6E75').text('Thank you for shopping with us.', { align: 'center' });
    doc.end();
  });
}

async function uploadInvoicePdf(buffer, number) {
  const { drive } = await clients();
  const { data } = await drive.files.create({
    requestBody: { name: `${number}.pdf`, mimeType: 'application/pdf', parents: [config.invoiceFolderId] },
    media: { mimeType: 'application/pdf', body: Readable.from(buffer) },
    fields: 'id,webViewLink',
  });
  return data;
}

async function deleteDriveFile(fileId) {
  if (!fileId) return;
  const { drive } = await clients();
  await drive.files.delete({ fileId }).catch(() => {});
}

app.get('/health', (_req, res) => res.json({ ok: true, service: 'sales-app-api', mode: 'preview' }));
app.use('/v1', authenticate);

app.get('/v1/products', async (req, res, next) => {
  try {
    const q = String(req.query.q ?? '').trim().toLowerCase();
    const table = await getTable('Inventory');
    const products = table.rows
      .filter(row => !q || String(row.SKU).toLowerCase().includes(q) || String(row['Product Name']).toLowerCase().includes(q))
      .slice(0, 40)
      .map(row => ({
        sku: String(row.SKU),
        name: String(row['Product Name'] ?? ''),
        category: String(row.Category ?? ''),
        price: Number(row['Selling Price'] ?? 0),
        stock: Number(row['Current Stock'] ?? 0),
        status: String(row['Stock Status'] ?? ''),
      }));
    res.json({ products });
  } catch (error) { next(error); }
});

app.get('/v1/customers', async (req, res, next) => {
  try {
    const phone = String(req.query.phone ?? '').trim();
    const q = String(req.query.q ?? '').trim().toLowerCase();
    const table = await getTable('Customers');
    const customer = phone ? table.rows.find(row => normalizedPhone(row.Phone) === normalizedPhone(phone)) : null;
    const customers = q ? table.rows.filter(row =>
      String(row['Customer Name'] ?? '').toLowerCase().includes(q) || normalizedPhone(row.Phone).includes(normalizedPhone(q))
    ).slice(0, 30).map(mapCustomer) : undefined;
    res.json({ customer: customer ? mapCustomer(customer) : null, ...(customers ? { customers } : {}) });
  } catch (error) { next(error); }
});

function mapCustomer(row) {
  return { id: String(row['Customer ID']), name: String(row['Customer Name'] ?? ''), phone: String(row.Phone ?? ''), email: String(row.Email ?? '') };
}

app.post('/v1/products', async (req, res, next) => {
  try {
    const name = String(req.body?.name ?? '').trim();
    const sellingPrice = Number(req.body?.sellingPrice);
    const stock = Number(req.body?.openingStock);
    if (!name || !Number.isFinite(sellingPrice) || sellingPrice < 0 || !Number.isInteger(stock) || stock < 0) {
      return res.status(400).json({ error: 'Enter a product name, valid selling price, and whole-number quantity.' });
    }
    const table = await getTable('Inventory');
    const duplicate = table.rows.find(row => String(row['Product Name']).trim().toLowerCase() === name.toLowerCase());
    if (duplicate) return res.status(409).json({ error: 'That product name already exists.', existingSku: duplicate.SKU });
    const sku = newId('SKU');
    const rowNumber = nextFreeRow(table);
    const formulas = formulasForRow('Inventory', rowNumber);
    await writeRecords([{
      title: 'Inventory', table, rowNumber,
      record: {
        SKU: sku,
        'Product Name': name,
        Category: String(req.body?.category ?? 'Other').trim() || 'Other',
        Supplier: String(req.body?.supplier ?? '').trim(),
        'Unit Cost': Number(req.body?.unitCost ?? 0),
        'Selling Price': sellingPrice,
        'Opening Stock': stock,
        'Reorder Level': Number(req.body?.reorderLevel ?? 0),
      }, formulas,
    }], ['Inventory']);
    res.status(201).json({ sku, name, price: sellingPrice, stock, qrValue: sku });
  } catch (error) { next(error); }
});

app.post('/v1/customers', async (req, res, next) => {
  try {
    const name = String(req.body?.name ?? '').trim();
    const phone = String(req.body?.phone ?? '').trim();
    if (!name || normalizedPhone(phone).length < 8) return res.status(400).json({ error: 'Enter a customer name and valid phone number.' });
    const existing = await findCustomer(phone);
    if (existing) return res.json({ customer: mapCustomer(existing), existing: true });
    const table = await getTable('Customers');
    const customer = {
      'Customer ID': newId('CUST'),
      'Customer Name': name,
      Phone: phone,
      Email: String(req.body?.email ?? '').trim(),
      'City / Address': String(req.body?.address ?? '').trim(),
      'Customer Type': String(req.body?.customerType ?? 'Retail').trim() || 'Retail',
    };
    const rowNumber = nextFreeRow(table);
    await writeRecords([{
      title: 'Customers', table, rowNumber, record: customer,
      formulas: formulasForRow('Customers', rowNumber),
    }], ['Customers']);
    res.status(201).json({ customer: { id: customer['Customer ID'], name, phone, email: customer.Email }, existing: false });
  } catch (error) { next(error); }
});

app.post('/v1/invoices', async (req, res, next) => {
  let uploadedFileId;
  try {
    const phone = String(req.body?.customer?.phone ?? '').trim();
    const name = String(req.body?.customer?.name ?? '').trim();
    const paymentMode = String(req.body?.paymentMode ?? '').trim();
    const items = req.body?.items;
    if (!phone || normalizedPhone(phone).length < 8) return res.status(400).json({ error: 'A valid customer phone number is required.' });
    if (!['Cash', 'UPI', 'Card'].includes(paymentMode)) return res.status(400).json({ error: 'Choose Cash, UPI, or Card.' });
    if (!Array.isArray(items) || items.length === 0) return res.status(400).json({ error: 'Add at least one product.' });

    const [inventory, customers, invoices, salesItems] = await Promise.all([
      getTable('Inventory'), getTable('Customers'), getTable('Sales Invoices'), getTable('Sales Items'),
    ]);
    let customer = customers.rows.find(row => normalizedPhone(row.Phone) === normalizedPhone(phone));
    const customerPlan = [];
    if (!customer) {
      if (!name) return res.status(400).json({ error: 'This number is new. Enter the customer name.' });
      const customerRow = nextFreeRow(customers);
      const record = {
        'Customer ID': newId('CUST'),
        'Customer Name': name,
        Phone: phone,
        Email: String(req.body?.customer?.email ?? '').trim(),
        'City / Address': String(req.body?.customer?.address ?? '').trim(),
        'Customer Type': 'Retail',
      };
      customer = { _rowNumber: customerRow, ...record };
      customerPlan.push({ title: 'Customers', table: customers, rowNumber: customerRow, record, formulas: formulasForRow('Customers', customerRow) });
    }

    const resolvedLines = items.map(item => {
      const product = inventory.rows.find(row => String(row.SKU) === String(item.sku));
      if (!product) throw Object.assign(new Error(`Product ${item.sku} was not found.`), { status: 400 });
      const quantity = Number(item.quantity);
      if (!Number.isInteger(quantity) || quantity < 1) throw Object.assign(new Error(`Choose a valid quantity for ${product['Product Name']}.`), { status: 400 });
      const stock = Number(product['Current Stock'] ?? 0);
      if (quantity > stock) throw Object.assign(new Error(`Only ${stock} units of ${product['Product Name']} are in stock.`), { status: 409 });
      const unitPrice = Number(product['Selling Price'] ?? 0);
      const unitCost = Number(product['Unit Cost'] ?? 0);
      const discount = Number(item.discount ?? 0);
      if (!Number.isFinite(discount) || discount < 0 || discount > unitPrice * quantity) throw Object.assign(new Error('Discount must be between zero and the item total.'), { status: 400 });
      return { sku: product.SKU, name: product['Product Name'], category: product.Category, quantity, unitPrice, unitCost, discount, lineTotal: Math.max(0, quantity * unitPrice - discount) };
    });

    const number = invoiceNumber();
    const date = new Date().toISOString().slice(0, 10);
    const total = resolvedLines.reduce((sum, line) => sum + line.lineTotal, 0);
    const invoice = { number, date, total, paymentMode };
    const customerSummary = { name: name || String(customer['Customer Name']), phone };
    const pdf = await buildInvoicePdf(invoice, customerSummary, resolvedLines);
    const file = await uploadInvoicePdf(pdf, number);
    uploadedFileId = file.id;

    const invoiceRow = nextFreeRow(invoices);
    const salesRowStart = nextFreeRow(salesItems);
    const plans = [
      ...customerPlan,
      {
        title: 'Sales Invoices', table: invoices, rowNumber: invoiceRow,
        record: { 'Invoice #': number, Date: sheetDateSerial(date), 'Customer ID': customer['Customer ID'], 'Payment Mode': paymentMode, 'Invoice File': file.webViewLink ?? `https://drive.google.com/file/d/${file.id}/view`, 'Customer Phone': phone },
        formulas: formulasForRow('Sales Invoices', invoiceRow),
      },
      ...resolvedLines.map((line, index) => {
        const rowNumber = salesRowStart + index;
        return {
          title: 'Sales Items', table: salesItems, rowNumber,
          record: { 'Invoice #': number, SKU: line.sku, Quantity: line.quantity, Discount: line.discount },
          formulas: formulasForRow('Sales Items', rowNumber),
        };
      }),
    ];
    await writeRecords(plans, ['Inventory', 'Customers', 'Sales Invoices', 'Sales Items']);
    uploadedFileId = undefined;
    res.status(201).json({ invoiceNumber: number, customer: customerSummary, items: resolvedLines, total, pdfUrl: file.webViewLink ?? '' });
  } catch (error) {
    await deleteDriveFile(uploadedFileId);
    next(error);
  }
});

app.use((error, _req, res, _next) => {
  const status = Number(error.status) || 500;
  if (status >= 500) console.error(error);
  res.status(status).json({ error: status >= 500 ? 'The request could not be completed. Try again or contact the manager.' : error.message });
});

const port = Number(process.env.PORT ?? 8080);
app.listen(port, '0.0.0.0', () => console.log(`Sales App API listening on ${port}`));
