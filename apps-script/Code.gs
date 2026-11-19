/**
 * Sales App API for Google Apps Script.
 *
 * Configure the Script Properties documented in apps-script/README.md, then
 * deploy this project as a web app that executes as its owner. The endpoint
 * validates each Google ID token against Google's token verification service
 * and an explicit staff allowlist before reading or writing Shop data.
 */

const APP = {
  maxRows: 1000,
  maxRequestBytes: 256 * 1024,
  timeZone: 'Asia/Kolkata',
  invoiceFolderName: 'Sales App Invoices',
  sheets: {
    Inventory: ['SKU', 'Product Name', 'Category', 'Supplier', 'Unit Cost', 'Selling Price', 'Opening Stock', 'Reorder Level', 'Purchased Qty', 'Sold Qty', 'Current Stock', 'Stock Value', 'Stock Status', 'QR Code', 'Image ID'],
    Customers: ['Customer ID', 'Customer Name', 'Phone', 'Email', 'City / Address', 'Customer Type', 'Total Orders', 'Total Spent', 'Last Purchase Date'],
    'Sales Invoices': ['Invoice #', 'Date', 'Customer ID', 'Customer Name', 'Items', 'Invoice Total', 'Gross Profit', 'Payment Mode', 'Invoice File', 'Customer Phone'],
    'Sales Items': ['Invoice #', 'Date', 'SKU', 'Product Name', 'Category', 'Quantity', 'Unit Price', 'Discount', 'Line Total', 'COGS', 'Gross Profit'],
  },
  primaryKey: { Inventory: 'SKU', Customers: 'Customer ID', 'Sales Invoices': 'Invoice #', 'Sales Items': 'Invoice #' },
};

function doGet() {
  return json_({ ok: true, service: 'sales-app-api', mode: 'apps-script' });
}

function doPost(event) {
  try {
    const raw = event && event.postData ? String(event.postData.contents || '') : '';
    if (!raw || raw.length > APP.maxRequestBytes) throw apiError_('Request is empty or too large.', 413);
    const request = JSON.parse(raw);
    const config = config_();
    const user = authenticate_(request.idToken, config);
    const payload = request.payload || {};
    let result;

    switch (String(request.action || '')) {
      case 'products':
        result = listProducts_(config, String(payload.query || ''));
        break;
      case 'findCustomer':
        result = { customer: findCustomer_(config, String(payload.phone || '')) };
        break;
      case 'listCustomers':
        result = { customers: listCustomers_(config) };
        break;
      case 'createCustomer':
        result = withWriteLock_(function () { return { customer: createCustomer_(config, payload) }; });
        break;
      case 'productImage':
        result = productImage_(payload);
        break;
      case 'listBills':
        result = { bills: listBills_(config, String(payload.phone || '')) };
        break;
      case 'listOrderRequests':
        result = { requests: listOrderRequests_(config, String(payload.phone || '')) };
        break;
      case 'createOrderRequest':
        result = withWriteLock_(function () { return createOrderRequest_(config, payload, user.email); });
        break;
      case 'createProduct':
        result = withWriteLock_(function () { return createProduct_(config, payload); });
        break;
      case 'createInvoice':
        result = withWriteLock_(function () { return createInvoice_(config, payload, user.email); });
        break;
      default:
        throw apiError_('Unknown Sales App action.', 404);
    }
    return json_(Object.assign({ ok: true }, result));
  } catch (error) {
    const message = error && error.apiMessage ? error.apiMessage : 'The request could not be completed. Try again or contact the manager.';
    if (!error || !error.apiMessage) console.error(error && error.stack ? error.stack : error);
    return json_({ ok: false, error: message });
  }
}

/** Run once from the Apps Script editor to create a private invoice folder. */
function setupInvoiceFolder() {
  const properties = PropertiesService.getScriptProperties();
  const config = config_(true);
  if (config.invoiceFolderId) return config.invoiceFolderId;
  const folder = Drive.Files.create({
    name: APP.invoiceFolderName,
    mimeType: 'application/vnd.google-apps.folder',
  }, null, { fields: 'id,name,webViewLink' });
  properties.setProperty('INVOICE_FOLDER_ID', folder.id);
  return folder.id;
}

function config_(allowMissingFolder) {
  const properties = PropertiesService.getScriptProperties();
  const config = {
    spreadsheetId: String(properties.getProperty('SHOP_SPREADSHEET_ID') || '').trim(),
    oauthClientId: String(properties.getProperty('GOOGLE_OAUTH_CLIENT_ID') || '').trim(),
    invoiceFolderId: String(properties.getProperty('INVOICE_FOLDER_ID') || '').trim(),
    authorizedEmails: new Set(String(properties.getProperty('AUTHORIZED_EMAILS') || '')
      .split(',').map(function (email) { return email.trim().toLowerCase(); }).filter(Boolean)),
  };
  if (!config.spreadsheetId || !config.oauthClientId || config.authorizedEmails.size === 0 || (!allowMissingFolder && !config.invoiceFolderId)) {
    throw apiError_('Sales App is not fully configured. Ask the manager to finish setup.', 503);
  }
  return config;
}

function authenticate_(idToken, config) {
  if (!idToken || String(idToken).length > 10000) throw apiError_('Sign in to use Sales App.', 401);
  // Apps Script has no built-in Google ID-token verifier. Google verifies the
  // token signature and claims; this endpoint then checks our OAuth audience
  // and staff allowlist. Requests fail closed if verification is unavailable.
  const response = UrlFetchApp.fetch('https://oauth2.googleapis.com/tokeninfo', {
    method: 'post',
    payload: { id_token: String(idToken) },
    muteHttpExceptions: true,
    followRedirects: false,
  });
  if (response.getResponseCode() !== 200) throw apiError_('Your sign-in expired. Sign in again.', 401);
  let claims;
  try { claims = JSON.parse(response.getContentText()); } catch (_) { throw apiError_('Could not verify your Google sign-in.', 401); }
  const email = String(claims.email || '').trim().toLowerCase();
  const expiry = Number(claims.exp || 0);
  if (claims.aud !== config.oauthClientId || !['accounts.google.com', 'https://accounts.google.com'].includes(String(claims.iss || '')) || expiry <= Math.floor(Date.now() / 1000)) {
    throw apiError_('Your sign-in is invalid or expired. Sign in again.', 401);
  }
  if (claims.email_verified !== 'true' && claims.email_verified !== true) throw apiError_('Use a verified Google account.', 403);
  if (!email || !config.authorizedEmails.has(email)) throw apiError_('This Google account is not allowed to use Sales App.', 403);
  return { email: email };
}

function listProducts_(config, query) {
  ensureProductImageColumn_(config);
  const normalizedQuery = query.trim().toLowerCase();
  const table = getTable_(config, 'Inventory');
  const products = table.rows.filter(function (row) {
    return !normalizedQuery || String(row.SKU).toLowerCase().includes(normalizedQuery) || String(row['Product Name']).toLowerCase().includes(normalizedQuery);
  }).slice(0, 1000).map(function (row) {
    return {
      sku: String(row.SKU),
      name: String(row['Product Name'] || ''),
      category: String(row.Category || ''),
      price: Number(row['Selling Price'] || 0),
      stock: Number(row['Current Stock'] || 0),
      status: String(row['Stock Status'] || ''),
      imageId: String(row['Image ID'] || ''),
    };
  });
  return { products: products };
}

function createCustomer_(config, payload) {
  const name = String(payload.name || '').trim();
  const phone = String(payload.phone || '').trim();
  const email = String(payload.email || '').trim();
  const address = String(payload.address || '').trim();
  if (!name) throw apiError_('Enter the customer name.', 400);
  if (normalizedPhone_(phone).length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  const table = getTable_(config, 'Customers');
  if (table.rows.some(function (row) { return normalizedPhone_(row.Phone) === normalizedPhone_(phone); })) {
    throw apiError_('A customer with this phone number already exists.', 409);
  }
  const customer = { 'Customer ID': newId_('CUS'), 'Customer Name': name, Phone: phone, Email: email, 'City / Address': address };
  writeRecords_(config, [{ title: 'Customers', table: table, rowNumber: nextFreeRow_(table), record: customer }], ['Customers']);
  return mapCustomer_(customer);
}

function listOrderRequests_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (digits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName('Order Requests');
  if (!sheet || sheet.getLastRow() < 2) return [];
  const values = sheet.getDataRange().getDisplayValues();
  const headers = values[0];
  const column = {};
  headers.forEach(function (header, index) { column[header] = index; });
  return values.slice(1).filter(function (row) { return normalizedPhone_(row[column['Customer Phone']]) === digits; })
    .reverse().slice(0, 100).map(function (row) {
      return {
        number: row[column['Request #']] || '', date: row[column.Date] || '',
        productName: row[column['Product Name']] || '', quantity: Number(row[column.Quantity] || 0),
        status: row[column.Status] || '', notes: row[column.Notes] || '',
      };
    });
}

function ensureProductImageColumn_(config) {
  const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName('Inventory');
  if (!sheet) throw apiError_('The Inventory tab was not found.', 500);
  const width = Math.max(1, sheet.getLastColumn());
  const headers = sheet.getRange(1, 1, 1, width).getDisplayValues()[0];
  if (headers.indexOf('Image ID') < 0) sheet.getRange(1, width + 1).setValue('Image ID');
}

function saveProductImage_(imageBase64, sku) {
  if (!imageBase64) return '';
  if (imageBase64.length > 160000) throw apiError_('Product picture is too large. Choose a smaller picture.', 413);
  const properties = PropertiesService.getScriptProperties();
  let folderId = String(properties.getProperty('PRODUCT_IMAGE_FOLDER_ID') || '').trim();
  if (!folderId) {
    const folder = Drive.Files.create({ name: 'Sales App Product Photos', mimeType: 'application/vnd.google-apps.folder' }, null, { fields: 'id' });
    folderId = folder.id;
    properties.setProperty('PRODUCT_IMAGE_FOLDER_ID', folderId);
  }
  const blob = Utilities.newBlob(Utilities.base64Decode(imageBase64), 'image/jpeg', sku + '.jpg');
  const file = Drive.Files.create({ name: sku + '.jpg', mimeType: 'image/jpeg', parents: [folderId] }, blob, { fields: 'id' });
  return file.id;
}

function productImage_(payload) {
  const imageId = String(payload.imageId || '').trim();
  if (!imageId) return { data: '' };
  const response = UrlFetchApp.fetch('https://www.googleapis.com/drive/v3/files/' + encodeURIComponent(imageId) + '?alt=media', {
    headers: { Authorization: 'Bearer ' + ScriptApp.getOAuthToken() },
    muteHttpExceptions: true,
  });
  if (response.getResponseCode() !== 200) throw apiError_('Could not load the product picture.', 404);
  const blob = response.getBlob();
  return { data: Utilities.base64Encode(blob.getBytes()), mimeType: blob.getContentType() };
}

function findCustomer_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (digits.length < 8) throw apiError_('Enter a valid phone number first.', 400);
  const table = getTable_(config, 'Customers');
  const row = table.rows.find(function (candidate) { return normalizedPhone_(candidate.Phone) === digits; });
  return row ? mapCustomer_(row) : null;
}

function listCustomers_(config) {
  return getTable_(config, 'Customers').rows.map(mapCustomer_);
}

function createOrderRequest_(config, payload, userEmail) {
  const productName = String(payload.productName || '').trim();
  const sku = String(payload.sku || '').trim();
  const customerPhone = String(payload.customerPhone || '').trim();
  const quantity = Number(payload.quantity);
  if (!productName) throw apiError_('Enter the product the customer wants.', 400);
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 10000) throw apiError_('Enter a whole-number quantity greater than zero.', 400);
  if (customerPhone && normalizedPhone_(customerPhone).length < 8) throw apiError_('Enter a valid customer phone number.', 400);

  const headers = ['Request #', 'Date', 'Customer Name', 'Customer Phone', 'SKU', 'Product Name', 'Quantity', 'Notes', 'Status', 'Requested By'];
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  const sheet = spreadsheet.getSheetByName('Order Requests') || spreadsheet.insertSheet('Order Requests');
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
  const requestNumber = newId_('REQ');
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss');
  sheet.appendRow([
    requestNumber,
    date,
    String(payload.customerName || '').trim(),
    customerPhone,
    sku,
    productName,
    quantity,
    String(payload.notes || '').trim(),
    'Pending',
    userEmail,
  ]);
  return { requestNumber: requestNumber, status: 'Pending' };
}

function listBills_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (phone.trim() && digits.length < 8) throw apiError_('Enter a valid phone number to search bills.', 400);
  const invoices = getTable_(config, 'Sales Invoices');
  const customers = getTable_(config, 'Customers');
  const salesItems = getTable_(config, 'Sales Items');
  const customerById = {};
  customers.rows.forEach(function (row) { customerById[String(row['Customer ID'])] = row; });
  const itemsByInvoice = {};
  salesItems.rows.forEach(function (row) {
    const invoiceNumber = String(row['Invoice #'] || '').trim();
    const sku = String(row.SKU || '').trim();
    const quantity = Number(row.Quantity);
    if (invoiceNumber && sku && Number.isInteger(quantity) && quantity > 0) {
      itemsByInvoice[invoiceNumber] = (itemsByInvoice[invoiceNumber] || 0) + 1;
    }
  });
  return invoices.rows
    .filter(function (row) {
      const invoiceNumber = String(row['Invoice #'] || '').trim();
      const customerPhone = normalizedPhone_(row['Customer Phone']);
      const total = Number(row['Invoice Total']);
      const paymentMode = String(row['Payment Mode'] || '').trim();
      return invoiceNumber && customerPhone.length >= 8 && Number.isFinite(total) && total > 0 &&
        (itemsByInvoice[invoiceNumber] || 0) > 0 && ['Cash', 'UPI', 'Card', 'Credit'].includes(paymentMode) &&
        (!digits || customerPhone === digits);
    })
    .sort(function (a, b) { return b._rowNumber - a._rowNumber; })
    .slice(0, 100)
    .map(function (row) {
      const customer = customerById[String(row['Customer ID'])] || {};
      const dateValue = row.Date;
      const date = typeof dateValue === 'number'
        ? Utilities.formatDate(new Date(Date.UTC(1899, 11, 30) + dateValue * 86400000), APP.timeZone, 'yyyy-MM-dd')
        : String(dateValue || '');
      return {
        invoiceNumber: String(row['Invoice #'] || ''),
        date: date,
        customerName: String(row['Customer Name'] || customer['Customer Name'] || ''),
        phone: String(row['Customer Phone'] || customer.Phone || ''),
        total: Number(row['Invoice Total'] || 0),
        paymentMode: String(row['Payment Mode'] || ''),
        pdfUrl: String(row['Invoice File'] || ''),
      };
    });
}

function createProduct_(config, payload) {
  ensureProductImageColumn_(config);
  const name = String(payload.name || '').trim();
  const category = String(payload.category || 'Other').trim() || 'Other';
  const sellingPrice = Number(payload.sellingPrice);
  const unitCost = Number(payload.unitCost || 0);
  const stock = Number(payload.openingStock);
  if (!name || !Number.isFinite(sellingPrice) || sellingPrice < 0 || !Number.isFinite(unitCost) || unitCost < 0 || !Number.isInteger(stock) || stock < 0) {
    throw apiError_('Enter a product name, valid prices, and a whole-number quantity.', 400);
  }
  const table = getTable_(config, 'Inventory');
  const duplicate = table.rows.find(function (row) { return String(row['Product Name']).trim().toLowerCase() === name.toLowerCase(); });
  if (duplicate) throw apiError_('That product name already exists. SKU: ' + duplicate.SKU, 409);
  const sku = newId_('SKU');
  const imageId = saveProductImage_(String(payload.imageBase64 || ''), sku);
  const rowNumber = nextFreeRow_(table);
  writeRecords_(config, [{
    title: 'Inventory', table: table, rowNumber: rowNumber,
    record: {
      SKU: sku,
      'Product Name': name,
      Category: category,
      Supplier: String(payload.supplier || '').trim(),
      'Unit Cost': unitCost,
      'Selling Price': sellingPrice,
      'Opening Stock': stock,
      'Reorder Level': Number(payload.reorderLevel || 0),
      'Image ID': imageId,
    },
  }], ['Inventory']);
  return { sku: sku, name: name, price: sellingPrice, stock: stock, imageId: imageId, qrValue: sku };
}

function createInvoice_(config, payload, userEmail) {
  const customerInput = payload.customer || {};
  const phone = String(customerInput.phone || '').trim();
  const name = String(customerInput.name || '').trim();
  const paymentMode = String(payload.paymentMode || '').trim();
  const items = payload.items;
  if (normalizedPhone_(phone).length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  if (!['Cash', 'UPI', 'Card'].includes(paymentMode)) throw apiError_('Choose Cash, UPI, or Card.', 400);
  if (!Array.isArray(items) || items.length === 0 || items.length > 100) throw apiError_('Add between 1 and 100 clothes before saving the bill.', 400);
  if (items.some(function (item) { return !item || typeof item !== 'object' || !String(item.sku || '').trim(); })) {
    throw apiError_('One or more bill items are missing a cloth code. Remove the incomplete item and add it again.', 400);
  }

  const inventory = getTable_(config, 'Inventory');
  const customers = getTable_(config, 'Customers');
  const invoices = getTable_(config, 'Sales Invoices');
  const salesItems = getTable_(config, 'Sales Items');
  let customer = customers.rows.find(function (row) { return normalizedPhone_(row.Phone) === normalizedPhone_(phone); });
  const plans = [];
  if (!customer) {
    if (!name) throw apiError_('This number is new. Enter the customer name.', 400);
    const customerRow = nextFreeRow_(customers);
    const customerRecord = {
      'Customer ID': newId_('CUST'),
      'Customer Name': name,
      Phone: phone,
      Email: String(customerInput.email || '').trim(),
      'City / Address': String(customerInput.address || '').trim(),
      'Customer Type': 'Retail',
    };
    customer = Object.assign({ _rowNumber: customerRow }, customerRecord);
    plans.push({ title: 'Customers', table: customers, rowNumber: customerRow, record: customerRecord });
  }

  const resolvedLines = items.map(function (item) {
    const product = inventory.rows.find(function (row) { return String(row.SKU) === String(item.sku); });
    if (!product) throw apiError_('Cloth ' + item.sku + ' was not found. Remove it from the bill and add it again.', 400);
    if (!String(product['Product Name'] || '').trim()) throw apiError_('A selected cloth is missing its name in Inventory. Ask the manager to correct it before billing.', 400);
    const quantity = Number(item.quantity);
    if (!Number.isInteger(quantity) || quantity < 1) throw apiError_('Enter a whole-number quantity for ' + product['Product Name'] + '.', 400);
    const stock = Number(product['Current Stock']);
    if (!Number.isFinite(stock) || stock < 0) throw apiError_('Stock information is incomplete for ' + product['Product Name'] + '. Ask the manager to update Inventory.', 400);
    if (quantity > stock) throw apiError_('Only ' + stock + ' units of ' + product['Product Name'] + ' are in stock.', 409);
    const unitPrice = Number(product['Selling Price']);
    if (!Number.isFinite(unitPrice) || unitPrice <= 0) throw apiError_('A valid selling price is missing for ' + product['Product Name'] + '. Ask the manager to correct it before billing.', 400);
    const unitCostValue = product['Unit Cost'] === '' ? 0 : Number(product['Unit Cost']);
    if (!Number.isFinite(unitCostValue) || unitCostValue < 0) throw apiError_('Cost information is invalid for ' + product['Product Name'] + '. Ask the manager to correct it before billing.', 400);
    const discount = Number(item.discount || 0);
    if (!Number.isFinite(discount) || discount < 0 || discount > unitPrice * quantity) throw apiError_('Discount must be between zero and the item total.', 400);
    return {
      sku: String(product.SKU), name: String(product['Product Name']), category: String(product.Category || ''),
      quantity: quantity, unitPrice: unitPrice, unitCost: unitCostValue, discount: discount,
      lineTotal: Math.max(0, quantity * unitPrice - discount),
    };
  });

  if (resolvedLines.length !== items.length || resolvedLines.some(function (line) {
    return !line.sku || !line.name || !Number.isInteger(line.quantity) || line.quantity < 1 ||
      !Number.isFinite(line.unitPrice) || line.unitPrice <= 0 || !Number.isFinite(line.lineTotal) || line.lineTotal <= 0;
  })) throw apiError_('Some bill details are incomplete. Check the cloth, price, and quantity, then try again.', 400);
  const total = resolvedLines.reduce(function (sum, line) { return sum + line.lineTotal; }, 0);
  if (!Number.isFinite(total) || total <= 0) throw apiError_('The bill total must be greater than zero. Check the item prices and discounts.', 400);
  if (!String(customer['Customer Name'] || name || '').trim()) throw apiError_('Enter the customer name before saving this bill.', 400);

  // Reserve enough distinct rows for every line before generating a PDF or writing anything.
  const invoiceRow = nextFreeRow_(invoices);
  const salesRows = nextFreeRows_(salesItems, resolvedLines.length);
  const number = invoiceNumber_();
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd');
  const invoice = { number: number, date: date, total: total, paymentMode: paymentMode };
  const customerSummary = { name: name || String(customer['Customer Name']), phone: phone };
  let createdPdfId = '';
  try {
    const pdf = buildInvoicePdf_(invoice, customerSummary, resolvedLines);
    const file = Drive.Files.create({
      name: number + '.pdf',
      mimeType: 'application/pdf',
      parents: [config.invoiceFolderId],
    }, pdf, { fields: 'id,webViewLink' });
    createdPdfId = file.id;
    shareInvoice_(file.id, config.authorizedEmails);

    plans.push({
      title: 'Sales Invoices', table: invoices, rowNumber: invoiceRow,
      record: {
        'Invoice #': number,
        Date: dateSerial_(date),
        'Customer ID': customer['Customer ID'],
        'Payment Mode': paymentMode,
        'Invoice File': file.webViewLink || 'https://drive.google.com/file/d/' + file.id + '/view',
        'Customer Phone': phone,
      },
    });
    resolvedLines.forEach(function (line, index) {
      plans.push({
        title: 'Sales Items', table: salesItems, rowNumber: salesRows[index],
        record: { 'Invoice #': number, SKU: line.sku, Quantity: line.quantity, Discount: line.discount },
      });
    });
    writeRecords_(config, plans, ['Inventory', 'Customers', 'Sales Invoices', 'Sales Items']);
    createdPdfId = '';
    return { invoiceNumber: number, customer: customerSummary, items: resolvedLines, total: total, pdfUrl: file.webViewLink || 'https://drive.google.com/file/d/' + file.id + '/view' };
  } catch (error) {
    if (createdPdfId) {
      try { Drive.Files.delete(createdPdfId); } catch (cleanupError) { console.error(cleanupError); }
    }
    throw error;
  }
}

function getTable_(config, title) {
  const required = APP.sheets[title];
  if (!required) throw apiError_('Unsupported sheet tab: ' + title, 500);
  const spreadsheet = Sheets.Spreadsheets.get(config.spreadsheetId, { fields: 'sheets(properties(sheetId,title))' });
  const properties = (spreadsheet.sheets || []).map(function (sheet) { return sheet.properties; }).find(function (sheet) { return sheet.title === title; });
  if (!properties) throw apiError_("Required tab '" + title + "' is missing from Shop.", 500);
  const range = "'" + title.replace(/'/g, "''") + "'!A1:Z" + APP.maxRows;
  const result = Sheets.Spreadsheets.Values.get(config.spreadsheetId, range, { valueRenderOption: 'UNFORMATTED_VALUE' });
  const values = result.values || [];
  const headers = (values[0] || []).map(String);
  const missing = required.filter(function (header) { return headers.indexOf(header) < 0; });
  if (missing.length) throw apiError_('Tab ' + title + ' is missing columns: ' + missing.join(', '), 500);
  const rows = [];
  for (let index = 1; index < APP.maxRows; index += 1) {
    const valuesRow = values[index] || [];
    const row = { _rowNumber: index + 1 };
    headers.forEach(function (header, column) { row[header] = valuesRow[column] === undefined ? '' : valuesRow[column]; });
    if (String(row[APP.primaryKey[title]] || '').trim()) rows.push(row);
  }
  return { sheetId: properties.sheetId, headers: headers, rows: rows, title: title };
}

function nextFreeRow_(table) {
  const occupied = new Set(table.rows.map(function (row) { return row._rowNumber; }));
  for (let rowNumber = 2; rowNumber <= APP.maxRows; rowNumber += 1) if (!occupied.has(rowNumber)) return rowNumber;
  throw apiError_('Tab has reached its current 1,000-row limit.', 409);
}

function nextFreeRows_(table, count) {
  const occupied = new Set(table.rows.map(function (row) { return row._rowNumber; }));
  const available = [];
  for (let rowNumber = 2; rowNumber <= APP.maxRows && available.length < count; rowNumber += 1) {
    if (!occupied.has(rowNumber)) available.push(rowNumber);
  }
  if (available.length !== count) throw apiError_('There is not enough room for all bill items. Ask the manager to make space in Sales Items.', 409);
  return available;
}

function writeRecords_(config, rowPlans, affectedTables) {
  const tables = {};
  Array.from(new Set(affectedTables.concat(rowPlans.map(function (plan) { return plan.title; }))).values()).forEach(function (title) {
    tables[title] = rowPlans.find(function (plan) { return plan.title === title; })?.table || getTable_(config, title);
  });
  const requests = [];
  rowPlans.forEach(function (plan) {
    requests.push.apply(requests, updateRowRequest_(plan.table, plan.rowNumber, plan.record));
  });
  affectedTables.forEach(function (title) {
    const table = tables[title];
    const plannedRows = rowPlans.filter(function (plan) { return plan.title === title; }).map(function (plan) { return plan.rowNumber; });
    const lastRow = Math.max(1, ...table.rows.map(function (row) { return row._rowNumber; }), ...plannedRows);
    const formulaSamples = formulasForRow_(title, 2);
    Object.keys(formulaSamples).forEach(function (header) {
      const columnIndex = table.headers.indexOf(header);
      if (columnIndex < 0 || lastRow < 2) return;
      const formulaRows = [];
      for (let row = 2; row <= lastRow; row += 1) formulaRows.push({ values: [{ userEnteredValue: { formulaValue: formulasForRow_(title, row)[header] } }] });
      requests.push({
        updateCells: {
          range: { sheetId: table.sheetId, startRowIndex: 1, endRowIndex: lastRow, startColumnIndex: columnIndex, endColumnIndex: columnIndex + 1 },
          rows: formulaRows,
          fields: 'userEnteredValue',
        },
      });
    });
    if (title === 'Inventory') {
      const qrIndex = table.headers.indexOf('QR Code');
      if (qrIndex >= 0) {
        const formula = '=MAP(A2:A1000,LAMBDA(sku,IF(sku="","",IMAGE("https://quickchart.io/qr?text="&ENCODEURL(sku)&"&size=150&margin=1))))';
        requests.push({ updateCells: { range: { sheetId: table.sheetId, startRowIndex: 1, endRowIndex: 2, startColumnIndex: qrIndex, endColumnIndex: qrIndex + 1 }, rows: [{ values: [{ userEnteredValue: { formulaValue: formula } }] }], fields: 'userEnteredValue' } });
      }
    }
  });
  if (requests.length) Sheets.Spreadsheets.batchUpdate({ requests: requests }, config.spreadsheetId);
}

function updateRowRequest_(table, rowNumber, record) {
  const values = table.headers.map(function () { return { userEnteredValue: { stringValue: '' } }; });
  Object.keys(record).forEach(function (header) {
    const index = table.headers.indexOf(header);
    if (index >= 0) values[index] = { userEnteredValue: cellValue_(record[header]) };
  });
  // Inventory's QR Code cell contains a spilling MAP formula. Never write into
  // the spill range; updating the SKU lets that formula produce the new QR.
  const qrIndex = table.title === 'Inventory' ? table.headers.indexOf('QR Code') : -1;
  if (qrIndex >= 0) {
    const requests = [];
    if (qrIndex > 0) requests.push(updateCellsRequest_(table, rowNumber, 0, qrIndex, values.slice(0, qrIndex)));
    if (qrIndex + 1 < table.headers.length) requests.push(updateCellsRequest_(table, rowNumber, qrIndex + 1, table.headers.length, values.slice(qrIndex + 1)));
    return requests;
  }
  return [updateCellsRequest_(table, rowNumber, 0, table.headers.length, values)];
}

function updateCellsRequest_(table, rowNumber, startColumnIndex, endColumnIndex, values) {
  return {
    updateCells: {
      range: { sheetId: table.sheetId, startRowIndex: rowNumber - 1, endRowIndex: rowNumber, startColumnIndex: startColumnIndex, endColumnIndex: endColumnIndex },
      rows: [{ values: values }],
      fields: 'userEnteredValue',
    },
  };
}

function formulasForRow_(title, row) {
  if (title === 'Inventory') return {
    'Purchased Qty': '=IF(A' + row + '="","",SUMIFS(\'Purchase Items\'!$F$2:$F$1000,\'Purchase Items\'!$C$2:$C$1000,A' + row + '))',
    'Sold Qty': '=IF(A' + row + '="","",SUMIFS(\'Sales Items\'!$F$2:$F$1000,\'Sales Items\'!$C$2:$C$1000,A' + row + '))',
    'Current Stock': '=IF(A' + row + '="","",G' + row + '+I' + row + '-J' + row + ')',
    'Stock Value': '=IF(A' + row + '="","",K' + row + '*E' + row + ')',
    'Stock Status': '=IF(A' + row + '="","",IF(K' + row + '<=0,"Out of Stock",IF(K' + row + '<=H' + row + ',"Low Stock","OK")))',
  };
  if (title === 'Customers') return {
    'Total Orders': '=IF(A' + row + '="","",COUNTIF(\'Sales Invoices\'!$C$2:$C$1000,A' + row + '))',
    'Total Spent': '=IF(A' + row + '="","",SUMIF(\'Sales Invoices\'!$C$2:$C$1000,A' + row + ',\'Sales Invoices\'!$F$2:$F$1000))',
    'Last Purchase Date': '=IF(A' + row + '="","",IF(COUNTIF(\'Sales Invoices\'!$C$2:$C$1000,A' + row + ')=0,"",MAXIFS(\'Sales Invoices\'!$B$2:$B$1000,\'Sales Invoices\'!$C$2:$C$1000,A' + row + ')))',
  };
  if (title === 'Sales Invoices') return {
    'Customer Name': '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Customers!$A$2:$I$1000,2,FALSE),""))',
    Items: '=IF(A' + row + '="","",COUNTIF(\'Sales Items\'!$A$2:$A$1000,A' + row + '))',
    'Invoice Total': '=IF(A' + row + '="","",SUMIF(\'Sales Items\'!$A$2:$A$1000,A' + row + ',\'Sales Items\'!$I$2:$I$1000))',
    'Gross Profit': '=IF(A' + row + '="","",SUMIF(\'Sales Items\'!$A$2:$A$1000,A' + row + ',\'Sales Items\'!$K$2:$K$1000))',
  };
  if (title === 'Sales Items') return {
    Date: '=IF(A' + row + '="","",IFERROR(VLOOKUP(A' + row + ',\'Sales Invoices\'!$A$2:$H$1000,2,FALSE),""))',
    'Product Name': '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,2,FALSE),""))',
    Category: '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,3,FALSE),""))',
    'Unit Price': '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,6,FALSE),""))',
    'Line Total': '=IF(OR(C' + row + '="",F' + row + '=""),"",MAX(0,F' + row + '*G' + row + '-H' + row + '))',
    COGS: '=IF(OR(C' + row + '="",F' + row + '=""),"",F' + row + '*IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,5,FALSE),0))',
    'Gross Profit': '=IF(OR(C' + row + '="",F' + row + '=""),"",I' + row + '-J' + row + ')',
  };
  return {};
}

function buildInvoicePdf_(invoice, customer, lines) {
  const escape = function (value) { return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]; }); };
  const bodyRows = lines.map(function (line) {
    return '<tr><td>' + escape(line.name) + '</td><td class="num">' + line.quantity + '</td><td class="num">₹' + line.unitPrice.toFixed(2) + '</td><td class="num">₹' + line.lineTotal.toFixed(2) + '</td></tr>';
  }).join('');
  const html = '<!doctype html><html><head><meta charset="utf-8"><style>' +
    'body{font-family:Arial,sans-serif;color:#153447;margin:36px}h1{text-align:center;color:#147d78;font-size:24px;margin:0} .muted{text-align:center;color:#62747a;margin:8px 0 28px} .customer{margin:24px 0}table{width:100%;border-collapse:collapse;margin-top:24px}th,td{padding:10px 8px;border-bottom:1px solid #d7e2e1;text-align:left}th{background:#edf4f3}.num{text-align:right}.total{text-align:right;color:#147d78;font-size:18px;font-weight:bold;margin-top:24px}.thanks{text-align:center;color:#62747a;margin-top:48px}' +
    '</style></head><body><h1>SALES INVOICE</h1><div class="muted">' + escape(invoice.number) + ' · ' + escape(invoice.date) + '</div>' +
    '<div class="customer"><b>Customer</b><br>' + escape(customer.name || 'Walk-in customer') + '<br>' + escape(customer.phone) + '<br>Payment: ' + escape(invoice.paymentMode) + '</div>' +
    '<table><thead><tr><th>Item</th><th class="num">Qty</th><th class="num">Price</th><th class="num">Total</th></tr></thead><tbody>' + bodyRows + '</tbody></table>' +
    '<div class="total">TOTAL&nbsp;&nbsp; ₹' + invoice.total.toFixed(2) + '</div><div class="thanks">Thank you for shopping with us.</div></body></html>';
  return HtmlService.createHtmlOutput(html).getBlob().getAs(MimeType.PDF).setName(invoice.number + '.pdf');
}

function shareInvoice_(fileId, authorizedEmails) {
  const ownerEmail = String(Session.getEffectiveUser().getEmail() || '').trim().toLowerCase();
  authorizedEmails.forEach(function (email) {
    if (email === ownerEmail) return;
    try {
      Drive.Permissions.create({ type: 'user', role: 'reader', emailAddress: email }, fileId, { sendNotificationEmail: false });
    } catch (error) {
      // The owner may already have access; permission failures for other staff
      // must not silently make the invoice public.
      if (!String(error && error.message || error).toLowerCase().includes('already')) throw error;
    }
  });
}

function mapCustomer_(row) {
  return { id: String(row['Customer ID'] || ''), name: String(row['Customer Name'] || ''), phone: String(row.Phone || ''), email: String(row.Email || '') };
}

function normalizedPhone_(value) {
  const digits = String(value || '').replace(/\D/g, '');
  return digits.length === 12 && digits.indexOf('91') === 0 ? digits.slice(-10) : digits;
}

function newId_(prefix) { return prefix + '-' + Utilities.getUuid().replace(/-/g, '').slice(0, 10).toUpperCase(); }

function invoiceNumber_() {
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyyMMdd');
  return 'INV-' + date + '-' + Utilities.getUuid().slice(0, 6).toUpperCase();
}

function dateSerial_(isoDate) {
  const parts = isoDate.split('-').map(Number);
  return (Date.UTC(parts[0], parts[1] - 1, parts[2]) - Date.UTC(1899, 11, 30)) / 86400000;
}

function cellValue_(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return { numberValue: value };
  if (typeof value === 'boolean') return { boolValue: value };
  return { stringValue: String(value == null ? '' : value) };
}

function withWriteLock_(callback) {
  const lock = LockService.getScriptLock();
  if (!lock.tryLock(20000)) throw apiError_('Another sale is being saved. Please try again in a moment.', 409);
  try { return callback(); } finally { lock.releaseLock(); }
}

function apiError_(message, status) {
  const error = new Error(message);
  error.apiMessage = message;
  error.status = status || 400;
  return error;
}

function json_(value) {
  return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON);
}
