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
    Inventory: ['SKU', 'Product Name', 'Category', 'Supplier', 'Unit Cost', 'Selling Price', 'Opening Stock', 'Reorder Level', 'Purchased Qty', 'Sold Qty', 'Current Stock', 'Stock Value', 'Stock Status', 'QR Code', 'Image ID', 'Availability', 'Fractional Quantity', 'Unit', 'Pricing Unit', 'Size / Variant', 'Date Added'],
    Customers: ['Customer ID', 'Customer Name', 'Phone', 'Email', 'City / Address', 'Customer Type', 'Total Orders', 'Total Spent', 'Last Purchase Date'],
    'Sales Invoices': ['Invoice #', 'Date', 'Customer ID', 'Customer Name', 'Items', 'Invoice Total', 'Gross Profit', 'Payment Mode', 'Invoice File', 'Customer Phone', 'Status'],
    'Sales Items': ['Invoice #', 'Date', 'SKU', 'Product Name', 'Category', 'Quantity', 'Unit Price', 'Discount', 'Line Total', 'COGS', 'Gross Profit', 'Status', 'Unit', 'Pricing Unit', 'Measure Quantity'],
    Payments: ['Payment ID', 'Date', 'Customer ID', 'Customer Phone', 'Invoice #', 'Amount', 'Payment Method', 'Received By', 'Notes', 'Receipt #', 'Client Request ID', 'Status'],
    'Stock Adjustments': ['Adjustment ID', 'Date', 'SKU', 'Quantity Added', 'Reason', 'Adjusted By'],
  },
  primaryKey: { Inventory: 'SKU', Customers: 'Customer ID', 'Sales Invoices': 'Invoice #', 'Sales Items': 'Invoice #', Payments: 'Payment ID', 'Stock Adjustments': 'Adjustment ID' },
};

function doGet(event) {
  if (event && event.parameter && event.parameter.health === '1') {
    return json_({ ok: true, service: 'sales-app-api', mode: 'apps-script' });
  }
  const page = HtmlService.createTemplateFromFile('Index');
  page.googleClientId = String(PropertiesService.getScriptProperties().getProperty('GOOGLE_OAUTH_CLIENT_ID') || '').trim();
  return page.evaluate()
    .setTitle('Avani Suto | Shop')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1');
}

function doPost(event) {
  const raw = event && event.postData ? String(event.postData.contents || '') : '';
  if (!raw || raw.length > APP.maxRequestBytes) return json_({ ok: false, error: 'Request is empty or too large.' });
  try {
    return json_(dispatchApi_(JSON.parse(raw)));
  } catch (error) {
    console.error(error && error.stack ? error.stack : error);
    return json_({ ok: false, error: 'The request could not be completed. Try again or contact the manager.' });
  }
}

/** Browser calls use google.script.run, avoiding cross-origin API restrictions. */
function webApi(action, payload, idToken) {
  return dispatchApi_({ action: action, payload: payload || {}, idToken: idToken });
}

function dispatchApi_(request) {
  try {
    const config = config_();
    const user = authenticate_(request && request.idToken, config);
    const payload = request && request.payload || {};
    let result;

    switch (String(request && request.action || '')) {
      case 'products':
        result = listProducts_(config, String(payload.query || ''));
        break;
      case 'session':
        result = { role: user.role, email: user.email };
        break;
      case 'requestDeletion':
        result = withWriteLock_(function () { return requestDeletion_(config, payload, user.email); });
        break;
      case 'listDeletionRequests':
        requireAdmin_(user);
        result = { requests: listDeletionRequests_(config, String(payload.type || '')) };
        break;
      case 'reviewDeletionRequest':
        requireAdmin_(user);
        result = withWriteLock_(function () { return reviewDeletionRequest_(config, payload, user.email); });
        break;
      case 'setProductAvailability':
        requireAdmin_(user);
        result = withWriteLock_(function () { return setProductAvailability_(config, payload); });
        break;
      case 'updateProduct':
        requireAdmin_(user);
        result = withWriteLock_(function () { return updateProduct_(config, payload); });
        break;
      case 'addStockAdjustment':
        requireAdmin_(user);
        result = withWriteLock_(function () { return addStockAdjustment_(config, payload, user.email); });
        break;
      case 'findCustomer':
        result = { customer: findCustomer_(config, String(payload.phone || '')) };
        break;
      case 'searchCustomers':
        result = { customers: searchCustomers_(config, String(payload.phone || '')) };
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
      case 'updateProductImage':
        result = withWriteLock_(function () { return updateProductImage_(config, payload); });
        break;
      case 'listBills':
        result = { bills: listBills_(config, String(payload.phone || '')) };
        break;
      case 'invoicePdf':
        result = invoicePdf_(config, payload);
        break;
      case 'generateInvoicePdf':
        result = withWriteLock_(function () { return generateInvoicePdf_(config, payload); });
        break;
      case 'customerFinance':
        result = customerFinance_(config, String(payload.phone || ''));
        break;
      case 'listPayments':
        result = { payments: listPayments_(config) };
        break;
      case 'receivePayment':
        result = withWriteLock_(function () { return receivePayment_(config, payload, user.email); });
        break;
      case 'receiveCustomerPayment':
        result = withWriteLock_(function () { return receiveCustomerPayment_(config, payload, user.email); });
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
    return Object.assign({ ok: true }, result);
  } catch (error) {
    const message = error && error.apiMessage ? error.apiMessage : 'The request could not be completed. Try again or contact the manager.';
    if (!error || !error.apiMessage) console.error(error && error.stack ? error.stack : error);
    return { ok: false, error: message };
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
    adminEmails: new Set(String(properties.getProperty('ADMIN_EMAILS') || 'keshrirajcp@gmail.com')
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
  return { email: email, role: config.adminEmails.has(email) ? 'ADMIN' : 'SALESPERSON' };
}

function requireAdmin_(user) {
  if (!user || user.role !== 'ADMIN') throw apiError_('Administrator access is required for this action.', 403);
}

function ensureColumn_(config, title, header) {
  ensureColumns_(config, title, [header]);
}

function ensureColumns_(config, title, headersToEnsure) {
  const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName(title);
  if (!sheet) throw apiError_("Required tab '" + title + "' is missing from Shop.", 500);
  const width = Math.max(1, sheet.getLastColumn());
  const headers = sheet.getRange(1, 1, 1, width).getDisplayValues()[0];
  const missing = headersToEnsure.filter(function (header) { return headers.indexOf(header) < 0; });
  if (missing.length) sheet.getRange(1, width + 1, 1, missing.length).setValues([missing]).setFontWeight('bold');
}

function getDeletionRequestsSheet_(config, create) {
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  let sheet = spreadsheet.getSheetByName('Deletion Requests');
  const headers = ['Request ID', 'Created At', 'Requested By', 'Record Type', 'Record ID', 'Summary', 'Reason', 'Status', 'Reviewed By', 'Reviewed At', 'Decision Note', 'Snapshot'];
  if (!sheet && create) sheet = spreadsheet.insertSheet('Deletion Requests');
  if (!sheet) return null;
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  } else {
    const existing = sheet.getRange(1, 1, 1, Math.max(1, sheet.getLastColumn())).getDisplayValues()[0];
    headers.forEach(function (header) {
      if (existing.indexOf(header) < 0) sheet.getRange(1, sheet.getLastColumn() + 1).setValue(header).setFontWeight('bold');
    });
  }
  return sheet;
}

function cancellationDate_(value) {
  if (value instanceof Date) value = Utilities.formatDate(value, APP.timeZone, 'yyyy-MM-dd');
  if (typeof value === 'number' && Number.isFinite(value)) {
    const date = new Date(Date.UTC(1899, 11, 30) + Math.floor(value) * 86400000);
    return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  }
  const text = String(value || '').trim();
  const iso = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) return new Date(Date.UTC(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3])));
  const parsed = new Date(text);
  if (Number.isNaN(parsed.getTime())) return null;
  return new Date(Date.UTC(parsed.getFullYear(), parsed.getMonth(), parsed.getDate()));
}

function isCancellationWithinDays_(recordDate, requestDate, maxDays) {
  const created = cancellationDate_(recordDate);
  const requested = cancellationDate_(requestDate);
  if (!created || !requested) return false;
  const age = Math.floor((requested.getTime() - created.getTime()) / 86400000);
  return age >= 0 && age <= maxDays;
}

function requestDeletion_(config, payload, userEmail) {
  const type = String(payload.type || '').trim().toUpperCase();
  const recordId = String(payload.recordId || '').trim();
  const reason = String(payload.reason || '').trim();
  if (!['PAYMENT', 'BILL', 'PRODUCT', 'ORDER'].includes(type)) throw apiError_('Choose a payment, bill, product, or order request.', 400);
  if (!recordId || recordId.length > 120) throw apiError_('Choose the entry you want corrected.', 400);
  if (reason.length < 4 || reason.length > 500) throw apiError_('Enter a short reason for this request.', 400);

  let summary = '';
  let snapshot = {};
  if (type === 'PAYMENT') {
    const payments = ensurePaymentsTable_(config);
    const rows = payments.rows.filter(function (row) {
      return String(row['Receipt #'] || '').trim() === recordId || String(row['Payment ID'] || '').trim() === recordId;
    });
    if (!rows.length || rows.every(isVoidedPayment_)) throw apiError_('That payment is no longer active in Shop.', 404);
    summary = '₹' + rows.reduce(function (sum, row) { return sum + Number(row.Amount || 0); }, 0) + ' · ' + String(rows[0]['Customer Phone'] || '');
    snapshot = rows.map(function (row) { return { id: row['Payment ID'], receipt: row['Receipt #'], invoice: row['Invoice #'], amount: row.Amount, status: row.Status || 'ACTIVE' }; });
  } else if (type === 'BILL') {
    ensureColumn_(config, 'Sales Invoices', 'Status');
    const row = getTable_(config, 'Sales Invoices').rows.find(function (item) { return String(item['Invoice #'] || '').trim() === recordId; });
    if (!row || String(row.Status || '').toUpperCase() === 'VOIDED') throw apiError_('That bill is no longer active in Shop.', 404);
    if (!isCancellationWithinDays_(row.Date, new Date(), 7)) throw apiError_('Bills can only be cancelled within 7 days of their date.', 409);
    summary = String(row['Customer Name'] || row['Customer Phone'] || '') + ' · ₹' + String(row['Invoice Total'] || '');
    snapshot = { invoice: recordId, customer: row['Customer Phone'], total: row['Invoice Total'], status: row.Status || 'ACTIVE' };
  } else if (type === 'ORDER') {
    const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName('Order Requests');
    if (!sheet || sheet.getLastRow() < 2) throw apiError_('That order request was not found.', 404);
    const values = sheet.getDataRange().getValues();
    const columns = headerIndexes_(values[0]);
    const matchingRows = values.slice(1).filter(function (row) { return String(row[columns['Request #']] || '').trim() === recordId; });
    if (!matchingRows.length) throw apiError_('That order request was not found.', 404);
    if (matchingRows.some(function (row) { return String(row[columns.Status] || '').toUpperCase() !== 'PENDING'; })) {
      throw apiError_('Only pending order requests can be cancelled.', 409);
    }
    summary = String(matchingRows[0][columns['Customer Name']] || matchingRows[0][columns['Customer Phone']] || '') + ' · ' + recordId;
    snapshot = { requestNumber: recordId, customerPhone: matchingRows[0][columns['Customer Phone']], status: 'PENDING' };
  } else {
    ensureColumn_(config, 'Inventory', 'Availability');
    const row = getTable_(config, 'Inventory').rows.find(function (item) { return String(item.SKU || '').trim() === recordId; });
    if (!row) throw apiError_('That cloth was not found in Shop.', 404);
    summary = String(row['Product Name'] || recordId);
    snapshot = { sku: recordId, name: row['Product Name'], availability: row.Availability || 'Available' };
  }

  const sheet = getDeletionRequestsSheet_(config, true);
  const values = sheet.getDataRange().getDisplayValues();
  const columns = headerIndexes_(values[0]);
  const pendingDuplicate = values.slice(1).some(function (row) {
    return String(row[columns['Record Type']] || '').toUpperCase() === type &&
      String(row[columns['Record ID']] || '') === recordId && String(row[columns.Status] || '').toUpperCase() === 'PENDING';
  });
  if (pendingDuplicate) throw apiError_('There is already a pending request for this entry.', 409);
  const requestId = newId_('DEL');
  const record = {
    'Request ID': requestId,
    'Created At': Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss'),
    'Requested By': userEmail,
    'Record Type': type,
    'Record ID': recordId,
    Summary: summary,
    Reason: reason,
    Status: 'PENDING',
    'Reviewed By': '',
    'Reviewed At': '',
    'Decision Note': '',
    Snapshot: JSON.stringify(snapshot),
  };
  const row = new Array(values[0].length).fill('');
  Object.keys(record).forEach(function (key) { row[columns[key]] = record[key]; });
  sheet.getRange(sheet.getLastRow() + 1, 1, 1, row.length).setValues([row]);
  return { request: {
    id: requestId, date: record['Created At'], requestedBy: userEmail, type: type,
    recordId: recordId, summary: summary, reason: reason, status: 'PENDING',
  } };
}

function listDeletionRequests_(config, type) {
  const sheet = getDeletionRequestsSheet_(config, false);
  if (!sheet || sheet.getLastRow() < 2) return [];
  const values = sheet.getDataRange().getDisplayValues();
  const columns = headerIndexes_(values[0]);
  const filterType = String(type || '').trim().toUpperCase();
  return values.slice(1).map(function (row) {
    return {
      id: row[columns['Request ID']] || '',
      date: row[columns['Created At']] || '',
      requestedBy: row[columns['Requested By']] || '',
      type: row[columns['Record Type']] || '',
      recordId: row[columns['Record ID']] || '',
      summary: row[columns.Summary] || '',
      reason: row[columns.Reason] || '',
      status: row[columns.Status] || '',
      reviewedBy: row[columns['Reviewed By']] || '',
      decisionNote: row[columns['Decision Note']] || '',
    };
  }).filter(function (request) {
    return request.id && (!filterType || request.type.toUpperCase() === filterType);
  }).reverse().slice(0, 100);
}

function reviewDeletionRequest_(config, payload, adminEmail) {
  const requestId = String(payload.requestId || '').trim();
  const decision = String(payload.decision || '').trim().toUpperCase();
  const decisionNote = String(payload.note || '').trim().slice(0, 500);
  if (!requestId || !['APPROVE', 'REJECT'].includes(decision)) throw apiError_('Choose approve or reject for the request.', 400);
  const sheet = getDeletionRequestsSheet_(config, false);
  if (!sheet) throw apiError_('There are no deletion requests in Shop.', 404);
  const values = sheet.getDataRange().getDisplayValues();
  const columns = headerIndexes_(values[0]);
  const index = values.findIndex(function (row, rowIndex) { return rowIndex > 0 && String(row[columns['Request ID']] || '') === requestId; });
  if (index < 1) throw apiError_('That request was not found.', 404);
  const rowNumber = index + 1;
  const row = values[index];
  if (String(row[columns.Status] || '').toUpperCase() !== 'PENDING') throw apiError_('That request has already been reviewed.', 409);
  if (decision === 'APPROVE') applyDeletionRequest_(config, row, columns);
  const status = decision === 'APPROVE' ? 'APPROVED' : 'REJECTED';
  sheet.getRange(rowNumber, columns.Status + 1).setValue(status);
  sheet.getRange(rowNumber, columns['Reviewed By'] + 1).setValue(adminEmail);
  sheet.getRange(rowNumber, columns['Reviewed At'] + 1).setValue(Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss'));
  sheet.getRange(rowNumber, columns['Decision Note'] + 1).setValue(decisionNote);
  return { requestId: requestId, status: status };
}

function applyDeletionRequest_(config, requestRow, columns) {
  const type = String(requestRow[columns['Record Type']] || '').toUpperCase();
  const recordId = String(requestRow[columns['Record ID']] || '').trim();
  if (type === 'PAYMENT') {
    const table = ensurePaymentsTable_(config);
    const matches = table.rows.filter(function (row) { return String(row['Receipt #'] || '').trim() === recordId || String(row['Payment ID'] || '').trim() === recordId; });
    if (!matches.length) throw apiError_('That payment was not found.', 404);
    matches.forEach(function (row) { setTableCell_(table, row._rowNumber, 'Status', 'VOIDED'); });
  } else if (type === 'BILL') {
    ensureColumn_(config, 'Sales Invoices', 'Status');
    ensureColumn_(config, 'Sales Items', 'Status');
    const invoices = getTable_(config, 'Sales Invoices');
    const invoice = invoices.rows.find(function (row) { return String(row['Invoice #'] || '').trim() === recordId; });
    if (!invoice) throw apiError_('That bill was not found.', 404);
    if (!isCancellationWithinDays_(invoice.Date, requestRow[columns['Created At']], 7)) throw apiError_('This bill cancellation request was not made within 7 days.', 409);
    if (String(invoice.Status || '').toUpperCase() !== 'VOIDED') setTableCell_(invoices, invoice._rowNumber, 'Status', 'VOIDED');
    const items = getTable_(config, 'Sales Items');
    items.rows.filter(function (row) { return String(row['Invoice #'] || '').trim() === recordId && String(row.Status || '').toUpperCase() !== 'VOIDED'; }).forEach(function (row) {
      setTableCell_(items, row._rowNumber, 'Status', 'VOIDED');
    });
    const payments = ensurePaymentsTable_(config);
    payments.rows.filter(function (row) { return String(row['Invoice #'] || '').trim() === recordId && !isVoidedPayment_(row); }).forEach(function (row) {
      setTableCell_(payments, row._rowNumber, 'Status', 'VOIDED');
    });
    writeRecords_(config, [], ['Sales Invoices', 'Sales Items', 'Inventory', 'Customers']);
  } else if (type === 'ORDER') {
    const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName('Order Requests');
    if (!sheet || sheet.getLastRow() < 2) throw apiError_('That order request was not found.', 404);
    const values = sheet.getDataRange().getValues();
    const columns = headerIndexes_(values[0]);
    const matchingRows = [];
    values.slice(1).forEach(function (row, index) {
      if (String(row[columns['Request #']] || '').trim() === recordId) matchingRows.push({ row: row, rowNumber: index + 2 });
    });
    if (!matchingRows.length) throw apiError_('That order request was not found.', 404);
    if (matchingRows.some(function (item) { return String(item.row[columns.Status] || '').toUpperCase() !== 'PENDING'; })) {
      throw apiError_('Only pending order requests can be cancelled.', 409);
    }
    matchingRows.forEach(function (item) { sheet.getRange(item.rowNumber, columns.Status + 1).setValue('CANCELLED'); });
  } else if (type === 'PRODUCT') {
    setProductAvailability_(config, { sku: recordId, available: false });
  } else {
    throw apiError_('This request type cannot be applied.', 400);
  }
}

function setProductAvailability_(config, payload) {
  const sku = String(payload.sku || '').trim();
  if (!sku) throw apiError_('Choose a cloth first.', 400);
  ensureColumn_(config, 'Inventory', 'Availability');
  const table = getTable_(config, 'Inventory');
  const product = table.rows.find(function (row) { return String(row.SKU || '').trim() === sku; });
  if (!product) throw apiError_('That cloth was not found in Shop.', 404);
  const available = payload.available === true || String(payload.available).toLowerCase() === 'true';
  setTableCell_(table, product._rowNumber, 'Availability', available ? 'Available' : 'Unavailable');
  return { sku: sku, available: available };
}

function updateProduct_(config, payload) {
  const sku = String(payload.sku || '').trim();
  const name = String(payload.name || '').trim();
  const sizeVariant = String(payload.sizeVariant || '').trim();
  const category = String(payload.category || 'Other').trim() || 'Other';
  const sellingPrice = Number(payload.sellingPrice);
  const unit = normalizedProductUnit_(payload.unit, payload.fractionalQuantity);
  const pricingUnit = normalizedPricingUnit_(payload.pricingUnit) || unit;
  const fractionalQuantity = payload.fractionalQuantity === true && unit !== 'PIECE';
  const allowedPricingUnits = unit === 'PIECE' ? ['PIECE', 'METRE', 'KG'] : [unit];
  if (allowedPricingUnits.indexOf(pricingUnit) < 0) throw apiError_('Price unit must match the metre or kg selling unit.', 400);
  if (!sku || !name || name.length > 120 || sizeVariant.length > 80 || category.length > 100 || !Number.isInteger(sellingPrice) || sellingPrice < 0) {
    throw apiError_('Enter a cloth name, category, and valid whole-rupee price.', 400);
  }
  ensureColumn_(config, 'Inventory', 'Fractional Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  ensureColumn_(config, 'Inventory', 'Size / Variant');
  const table = getTable_(config, 'Inventory');
  const row = table.rows.find(function (item) { return String(item.SKU || '').trim() === sku; });
  if (!row) throw apiError_('That cloth was not found in Shop.', 404);
  const duplicate = table.rows.find(function (item) {
    return String(item.SKU || '').trim() !== sku &&
      String(item['Product Name'] || '').trim().toLowerCase() === name.toLowerCase() &&
      String(item['Size / Variant'] || '').trim().toLowerCase() === sizeVariant.toLowerCase();
  });
  if (duplicate) throw apiError_('That cloth name and size / variant already exist.', 409);
  setTableCell_(table, row._rowNumber, 'Product Name', name);
  setTableCell_(table, row._rowNumber, 'Size / Variant', sizeVariant);
  setTableCell_(table, row._rowNumber, 'Category', category);
  setTableCell_(table, row._rowNumber, 'Selling Price', sellingPrice);
  setTableCell_(table, row._rowNumber, 'Fractional Quantity', fractionalQuantity);
  setTableCell_(table, row._rowNumber, 'Unit', unit);
  setTableCell_(table, row._rowNumber, 'Pricing Unit', pricingUnit);
  return { sku: sku, name: name, sizeVariant: sizeVariant, category: category, price: sellingPrice, fractionalQuantity: fractionalQuantity, unit: unit, pricingUnit: pricingUnit };
}

function ensureStockAdjustmentsSheet_(config) {
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  let sheet = spreadsheet.getSheetByName('Stock Adjustments');
  const headers = APP.sheets['Stock Adjustments'];
  if (!sheet) sheet = spreadsheet.insertSheet('Stock Adjustments');
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
}

function addStockAdjustment_(config, payload, adminEmail) {
  ensureStockAdjustmentsSheet_(config);
  const sku = String(payload.sku || '').trim();
  const quantity = Number(payload.quantity);
  const reason = String(payload.reason || 'Restock').trim().slice(0, 200) || 'Restock';
  if (!sku || !Number.isFinite(quantity) || quantity <= 0 || quantity > 100000) {
    throw apiError_('Enter a stock quantity greater than zero.', 400);
  }
  const inventory = getTable_(config, 'Inventory');
  const product = inventory.rows.find(function (row) { return String(row.SKU || '').trim() === sku; });
  if (!product) throw apiError_('That cloth was not found in Shop.', 404);
  if (!fractionalQuantityEnabled_(product['Fractional Quantity']) && !Number.isInteger(quantity)) {
    throw apiError_('This cloth is counted in whole pieces. Enter a whole number.', 400);
  }
  const table = getTable_(config, 'Stock Adjustments');
  const record = {
    'Adjustment ID': newId_('STK'),
    Date: Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss'),
    SKU: sku,
    'Quantity Added': quantity,
    Reason: reason,
    'Adjusted By': adminEmail,
  };
  writeRecords_(config, [
    { title: 'Stock Adjustments', table: table, rowNumber: nextFreeRow_(table), record: record },
  ], ['Stock Adjustments', 'Inventory']);
  return { sku: sku, quantityAdded: quantity };
}

function setTableCell_(table, rowNumber, header, value) {
  const column = table.headers.indexOf(header);
  if (column < 0) throw apiError_('Shop is missing the ' + header + ' column.', 500);
  Sheets.Spreadsheets.batchUpdate({ requests: [{ updateCells: {
    range: { sheetId: table.sheetId, startRowIndex: rowNumber - 1, endRowIndex: rowNumber, startColumnIndex: column, endColumnIndex: column + 1 },
    rows: [{ values: [{ userEnteredValue: cellValue_(value) }] }], fields: 'userEnteredValue',
  } }] }, table.spreadsheetId || configSpreadsheetIdFromTable_(table));
}

function configSpreadsheetIdFromTable_(table) {
  return table.spreadsheetId;
}

function headerIndexes_(headers) {
  const indexes = {};
  (headers || []).forEach(function (header, index) { indexes[String(header)] = index; });
  return indexes;
}

function isVoidedPayment_(row) {
  return String(row.Status || '').trim().toUpperCase() === 'VOIDED';
}

function fractionalQuantityEnabled_(value) {
  return value === true || ['true', 'yes', 'y', '1'].indexOf(String(value || '').trim().toLowerCase()) >= 0;
}

function normalizedProductUnit_(value, fractionalQuantity) {
  const unit = String(value || '').trim().toUpperCase();
  if (['KG', 'KGS', 'KILOGRAM', 'KILOGRAMS'].indexOf(unit) >= 0) return 'KG';
  if (['M', 'METRE', 'METRES', 'METER', 'METERS'].indexOf(unit) >= 0) return 'METRE';
  if (['PIECE', 'PIECES', 'PCS', 'PC', 'UNIT'].indexOf(unit) >= 0) return 'PIECE';
  return fractionalQuantityEnabled_(fractionalQuantity) ? 'METRE' : 'PIECE';
}

function productUnit_(row) {
  return normalizedProductUnit_(row && row.Unit, row && row['Fractional Quantity']);
}

function listProducts_(config, query) {
  ensureProductImageColumn_(config);
  ensureColumn_(config, 'Inventory', 'Availability');
  ensureColumn_(config, 'Inventory', 'Fractional Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  ensureColumn_(config, 'Inventory', 'Size / Variant');
  ensureColumn_(config, 'Inventory', 'Date Added');
  const normalizedQuery = query.trim().toLowerCase();
  if (!normalizedQuery) voidOrphanedSalesItems_(config);
  const table = getTable_(config, 'Inventory');
  const products = table.rows.filter(function (row) {
    return !normalizedQuery || String(row.SKU).toLowerCase().includes(normalizedQuery) || String(row['Product Name']).toLowerCase().includes(normalizedQuery) || String(row['Size / Variant'] || '').toLowerCase().includes(normalizedQuery);
  }).slice(0, 1000).map(function (row) {
    return {
      sku: String(row.SKU),
      name: String(row['Product Name'] || ''),
      sizeVariant: String(row['Size / Variant'] || ''),
      dateAdded: productDateAdded_(row['Date Added']),
      category: String(row.Category || ''),
      price: Number(row['Selling Price'] || 0),
      costPrice: Number(row['Unit Cost'] || 0),
      stock: Number(row['Current Stock'] || 0),
      fractionalQuantity: fractionalQuantityEnabled_(row['Fractional Quantity']),
      unit: productUnit_(row),
      pricingUnit: normalizedPricingUnit_(row['Pricing Unit']),
      status: String(row['Stock Status'] || ''),
      imageId: String(row['Image ID'] || ''),
      available: String(row.Availability || 'Available').trim().toLowerCase() !== 'unavailable' && String(row.Availability || 'Available').trim().toLowerCase() !== 'false',
    };
  });
  return { products: products };
}

function voidOrphanedSalesItems_(config) {
  const tables = getTables_(config, ['Sales Invoices', 'Sales Items']);
  const activeInvoices = new Set(tables['Sales Invoices'].rows
    .filter(function (row) { return String(row.Status || '').toUpperCase() !== 'VOIDED'; })
    .map(function (row) { return String(row['Invoice #'] || '').trim(); }));
  const items = tables['Sales Items'];
  const statusColumn = items.headers.indexOf('Status');
  const requests = items.rows.filter(function (row) {
    return String(row.Status || '').toUpperCase() !== 'VOIDED' &&
      !activeInvoices.has(String(row['Invoice #'] || '').trim());
  }).map(function (row) {
    return { updateCells: {
      range: { sheetId: items.sheetId, startRowIndex: row._rowNumber - 1, endRowIndex: row._rowNumber, startColumnIndex: statusColumn, endColumnIndex: statusColumn + 1 },
      rows: [{ values: [{ userEnteredValue: { stringValue: 'VOIDED' } }] }],
      fields: 'userEnteredValue',
    } };
  });
  if (requests.length) {
    Sheets.Spreadsheets.batchUpdate({ requests: requests }, config.spreadsheetId);
    SpreadsheetApp.flush();
  }
}

function createCustomer_(config, payload) {
  const name = String(payload.name || '').trim();
  const phone = String(payload.phone || '').trim();
  const email = String(payload.email || '').trim();
  const address = String(payload.address || '').trim();
  const customerType = String(payload.customerType || '').trim().toLowerCase() === 'shop owner' ? 'Shop Owner' : 'Retail';
  if (!name) throw apiError_('Enter the customer name.', 400);
  if (normalizedPhone_(phone).length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  const table = getTable_(config, 'Customers');
  if (table.rows.some(function (row) { return normalizedPhone_(row.Phone) === normalizedPhone_(phone); })) {
    throw apiError_('A customer with this phone number already exists.', 409);
  }
  const customer = { 'Customer ID': newId_('CUS'), 'Customer Name': name, Phone: phone, Email: email, 'City / Address': address, 'Customer Type': customerType };
  writeRecords_(config, [{ title: 'Customers', table: table, rowNumber: nextFreeRow_(table), record: customer }], ['Customers']);
  return mapCustomer_(customer);
}

function listOrderRequests_(config, phone) {
  const rawPhone = String(phone || '').trim();
  const digits = normalizedPhone_(phone);
  if (rawPhone && digits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  const sheet = SpreadsheetApp.openById(config.spreadsheetId).getSheetByName('Order Requests');
  if (!sheet || sheet.getLastRow() < 2) return [];
  const values = sheet.getDataRange().getDisplayValues();
  const headers = values[0];
  const column = {};
  headers.forEach(function (header, index) { column[header] = index; });
  const grouped = {};
  values.slice(1).forEach(function (row, index) {
    if (digits && normalizedPhone_(row[column['Customer Phone']]) !== digits) return;
    const number = row[column['Request #']] || '';
    if (!number) return;
    if (!grouped[number]) grouped[number] = {
      number: number, date: row[column.Date] || '', customerName: row[column['Customer Name']] || '',
      customerPhone: row[column['Customer Phone']] || '', items: [], quantity: 0,
      status: row[column.Status] || '', notes: row[column.Notes] || '', lastRow: index + 2,
    };
    const group = grouped[number];
    const productName = row[column['Product Name']] || '';
    const quantity = Number(row[column.Quantity] || 0);
    group.items.push({ name: productName, sku: row[column.SKU] || '', quantity: quantity });
    group.quantity += quantity;
  });
  return Object.keys(grouped).map(function (number) {
    const group = grouped[number];
    return {
      number: group.number, date: group.date,
      customerName: group.customerName, customerPhone: group.customerPhone,
      productName: group.items.map(function (item) { return item.name + ' ×' + item.quantity; }).join(', '),
      items: group.items, quantity: group.quantity, status: group.status, notes: group.notes, lastRow: group.lastRow,
    };
  }).sort(function (a, b) { return b.lastRow - a.lastRow; }).slice(0, 100)
    .map(function (group) {
      return { number: group.number, date: group.date, customerName: group.customerName, customerPhone: group.customerPhone, productName: group.productName, items: group.items, quantity: group.quantity, status: group.status, notes: group.notes };
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

function updateProductImage_(config, payload) {
  const sku = String(payload.sku || '').trim();
  const imageBase64 = String(payload.imageBase64 || '');
  if (!sku) throw apiError_('Choose a product before adding its picture.', 400);
  if (!imageBase64) throw apiError_('Choose a picture before saving.', 400);
  ensureProductImageColumn_(config);
  const table = getTable_(config, 'Inventory');
  const row = table.rows.find(function (item) { return String(item.SKU || '').trim() === sku; });
  if (!row) throw apiError_('This product was not found in Shop. Sync products and try again.', 404);
  const imageId = saveProductImage_(imageBase64, sku);
  setTableCell_(table, row._rowNumber, 'Image ID', imageId);
  return { sku: sku, imageId: imageId };
}

function findCustomer_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (digits.length < 8) throw apiError_('Enter a valid phone number first.', 400);
  const table = getTable_(config, 'Customers');
  const row = table.rows.find(function (candidate) { return normalizedPhone_(candidate.Phone) === digits; });
  return row ? mapCustomer_(row) : null;
}

function searchCustomers_(config, phonePrefix) {
  const digits = normalizedPhone_(phonePrefix);
  if (digits.length < 3) return [];
  return getTable_(config, 'Customers').rows
    .filter(function (row) { return normalizedPhone_(row.Phone).indexOf(digits) === 0; })
    .slice(0, 8)
    .map(mapCustomer_);
}

function listCustomers_(config) {
  return getTable_(config, 'Customers').rows.map(mapCustomer_);
}

function createOrderRequest_(config, payload, userEmail) {
  const items = Array.isArray(payload.items) ? payload.items : [{
    productName: payload.productName,
    sku: payload.sku,
    quantity: payload.quantity,
  }];
  const customerPhone = String(payload.customerPhone || '').trim();
  const customerName = String(payload.customerName || '').trim();
  const phoneDigits = normalizedPhone_(customerPhone);
  if (phoneDigits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  if (!Array.isArray(items) || items.length < 1) throw apiError_('Add at least one cloth before sending the order.', 400);
  const inventory = getTable_(config, 'Inventory');
  const resolvedItems = items.map(function (item) {
    const productName = String(item && item.productName || '').trim();
    const sku = String(item && item.sku || '').trim();
    const quantity = Number(item && item.quantity);
    if (!productName || !sku) throw apiError_('An order item is missing its cloth name or code. Remove it and add it again.', 400);
    const product = inventory.rows.find(function (row) { return String(row.SKU || '') === sku; });
    if (!product || String(product.Availability || 'Available').toLowerCase() === 'unavailable' || String(product.Availability || '').toLowerCase() === 'false') throw apiError_('That cloth is unavailable. Sync the catalog and choose another cloth.', 409);
    const fractionalAllowed = fractionalQuantityEnabled_(product['Fractional Quantity']);
    if (!Number.isFinite(quantity) || quantity < 0.001 || (!fractionalAllowed && !Number.isInteger(quantity)) || (fractionalAllowed && Math.abs(quantity * 1000 - Math.round(quantity * 1000)) > 0.000001)) {
      throw apiError_((fractionalAllowed ? 'Enter a positive quantity with up to three decimal places for ' : 'Enter a whole-number quantity greater than zero for ') + productName + '.', 400);
    }
    return { productName: productName, sku: sku, quantity: quantity };
  });
  if (!customerName) {
    const customers = getTable_(config, 'Customers');
    const existing = customers.rows.find(function (row) { return normalizedPhone_(row.Phone) === phoneDigits; });
    if (!existing) throw apiError_('Enter the customer name before sending this order.', 400);
  }

  const headers = ['Request #', 'Date', 'Customer Name', 'Customer Phone', 'SKU', 'Product Name', 'Quantity', 'Notes', 'Status', 'Requested By'];
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  const sheet = spreadsheet.getSheetByName('Order Requests') || spreadsheet.insertSheet('Order Requests');
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
  const requiredLastRow = sheet.getLastRow() + resolvedItems.length;
  if (requiredLastRow > sheet.getMaxRows()) {
    sheet.insertRowsAfter(sheet.getMaxRows(), requiredLastRow - sheet.getMaxRows());
  }

  const customers = getTable_(config, 'Customers');
  let customer = customers.rows.find(function (row) { return normalizedPhone_(row.Phone) === phoneDigits; });
  if (!customer) {
    const created = createCustomer_(config, {
      name: customerName,
      phone: customerPhone,
      email: String(payload.customerEmail || '').trim(),
      address: String(payload.customerAddress || '').trim(),
    });
    customer = { 'Customer Name': created.name };
  }
  const requestNumber = newId_('REQ');
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss');
  const rows = resolvedItems.map(function (item) { return [
    requestNumber, date, String(customer['Customer Name'] || customerName), customerPhone,
    item.sku, item.productName, item.quantity, String(payload.notes || '').trim(), 'Pending', userEmail,
  ]; });
  sheet.getRange(sheet.getLastRow() + 1, 1, rows.length, headers.length).setValues(rows);
  return { requestNumber: requestNumber, status: 'Pending' };
}

function listBills_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (phone.trim() && digits.length < 8) throw apiError_('Enter a valid phone number to search bills.', 400);
  const invoices = getTable_(config, 'Sales Invoices');
  const customers = getTable_(config, 'Customers');
  ensureColumn_(config, 'Sales Items', 'Unit');
  ensureColumn_(config, 'Sales Items', 'Pricing Unit');
  ensureColumn_(config, 'Sales Items', 'Measure Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  const salesItems = getTable_(config, 'Sales Items');
  const payments = readPaymentsTable_(config);
  const inventory = getTable_(config, 'Inventory');
  const inventoryBySku = {};
  inventory.rows.forEach(function (row) { inventoryBySku[String(row.SKU || '')] = row; });
  const paidByInvoice = paymentsByInvoice_(payments);
  const customerById = {};
  customers.rows.forEach(function (row) { customerById[String(row['Customer ID'])] = row; });
  const itemsByInvoice = {};
  const invoiceLinesByInvoice = {};
  salesItems.rows.forEach(function (row) {
    const invoiceNumber = String(row['Invoice #'] || '').trim();
    const sku = String(row.SKU || '').trim();
    const quantity = Number(row.Quantity);
    if (invoiceNumber && sku && Number.isFinite(quantity) && quantity > 0) {
      itemsByInvoice[invoiceNumber] = (itemsByInvoice[invoiceNumber] || 0) + 1;
      if (!invoiceLinesByInvoice[invoiceNumber]) invoiceLinesByInvoice[invoiceNumber] = [];
      invoiceLinesByInvoice[invoiceNumber].push({
        sku: sku,
        name: String(row['Product Name'] || ''),
        category: String(row.Category || ''),
        quantity: quantity,
        fractionalQuantity: fractionalQuantityEnabled_((inventoryBySku[sku] || {})['Fractional Quantity']),
        unit: normalizedProductUnit_(row.Unit, (inventoryBySku[sku] || {})['Fractional Quantity']),
        measureQuantity: Number(row['Measure Quantity'] || 0),
        pricingUnit: normalizedPricingUnit_(row['Pricing Unit']),
        unitPrice: Number(row['Unit Price'] || 0),
        discount: Number(row.Discount || 0),
        lineTotal: Number(row['Line Total'] || 0),
        voided: String(row.Status || '').toUpperCase() === 'VOIDED',
      });
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
      const invoiceNumber = String(row['Invoice #'] || '');
      const total = Number(row['Invoice Total'] || 0);
      const voided = String(row.Status || '').toUpperCase() === 'VOIDED';
      const paid = voided ? 0 : paidAmountForInvoice_(payments, paidByInvoice, invoiceNumber, row, total);
      const dateValue = row.Date;
      const date = typeof dateValue === 'number'
        ? Utilities.formatDate(new Date(Date.UTC(1899, 11, 30) + dateValue * 86400000), APP.timeZone, 'yyyy-MM-dd')
        : String(dateValue || '');
      return {
        invoiceNumber: String(row['Invoice #'] || ''),
        date: date,
        customerName: String(row['Customer Name'] || customer['Customer Name'] || ''),
        phone: String(row['Customer Phone'] || customer.Phone || ''),
        total: total,
        paymentMode: String(row['Payment Mode'] || ''),
        pdfUrl: String(row['Invoice File'] || ''),
        paid: Math.min(total, paid),
        due: voided ? 0 : Math.max(0, total - paid),
        voided: voided,
        items: invoiceLinesByInvoice[invoiceNumber] || [],
      };
    });
}

function invoicePdf_(config, payload) {
  const invoiceNumber = String(payload.invoiceNumber || '').trim();
  if (!invoiceNumber) throw apiError_('Choose a bill before sharing its PDF.', 400);
  const invoiceRow = getTable_(config, 'Sales Invoices').rows.find(function (row) {
    return String(row['Invoice #'] || '').trim() === invoiceNumber;
  });
  if (!invoiceRow) throw apiError_('This bill was not found in Shop. Sync bills and try again.', 404);
  if (String(invoiceRow.Status || '').toUpperCase() === 'VOIDED') throw apiError_('This bill has been cancelled by the administrator.', 409);
  let fileUrl = String(invoiceRow['Invoice File'] || '').trim();
  let match = fileUrl.match(/\/file\/d\/([A-Za-z0-9_-]+)/) || fileUrl.match(/[?&]id=([A-Za-z0-9_-]+)/);
  if (!match) {
    const generated = generateInvoicePdf_(config, { invoiceNumber: invoiceNumber });
    fileUrl = generated.pdfUrl;
    match = fileUrl.match(/\/file\/d\/([A-Za-z0-9_-]+)/) || fileUrl.match(/[?&]id=([A-Za-z0-9_-]+)/);
  }
  if (!match) throw apiError_('The bill PDF is still being prepared. Try again shortly.', 503);
  const fileId = match[1];
  const metadata = Drive.Files.get(fileId, { fields: 'id,mimeType,parents,name' });
  if (metadata.mimeType !== 'application/pdf' || !Array.isArray(metadata.parents) || metadata.parents.indexOf(config.invoiceFolderId) < 0) {
    throw apiError_('The bill PDF is not available in the private invoice folder.', 404);
  }
  const total = Number(invoiceRow['Invoice Total'] || 0);
  const payments = readPaymentsTable_(config);
  const paidByInvoice = paymentsByInvoice_(payments);
  const recordedPaid = paidAmountForInvoice_(payments, paidByInvoice, invoiceNumber, invoiceRow, total);
  const paid = Math.min(total, recordedPaid);
  const dateValue = invoiceRow.Date;
  const date = typeof dateValue === 'number'
    ? Utilities.formatDate(new Date(Date.UTC(1899, 11, 30) + dateValue * 86400000), APP.timeZone, 'yyyy-MM-dd')
    : String(dateValue || '');
  ensureColumn_(config, 'Sales Items', 'Unit');
  ensureColumn_(config, 'Sales Items', 'Pricing Unit');
  ensureColumn_(config, 'Sales Items', 'Measure Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  const itemRows = getTable_(config, 'Sales Items').rows.filter(function (row) {
    return String(row['Invoice #'] || '').trim() === invoiceNumber;
  });
  const lines = itemRows.map(function (row) {
    const quantity = Number(row.Quantity || 0);
    const unitPrice = Number(row['Unit Price'] || 0);
    const discount = Number(row.Discount || 0);
    const product = getTable_(config, 'Inventory').rows.find(function (candidate) { return String(candidate.SKU || '') === String(row.SKU || ''); }) || {};
    return {
      sku: String(row.SKU || ''), name: String(row['Product Name'] || ''), category: String(row.Category || ''),
      quantity: quantity, fractionalQuantity: fractionalQuantityEnabled_(product['Fractional Quantity']), unit: normalizedProductUnit_(row.Unit, product['Fractional Quantity']), unitPrice: unitPrice, unitCost: 0, discount: discount,
      measureQuantity: Number(row['Measure Quantity'] || 0), pricingUnit: normalizedPricingUnit_(row['Pricing Unit']),
      lineTotal: Number(row['Line Total'] || Math.max(0, invoicePriceQuantity_(quantity, Number(row['Measure Quantity'] || 0), normalizedProductUnit_(row.Unit, product['Fractional Quantity']), normalizedPricingUnit_(row['Pricing Unit']) || productUnit_(product)) * unitPrice - discount)),
    };
  });
  if (!lines.length || lines.some(function (line) { return !line.name || line.quantity < 0.001 || line.unitPrice <= 0; })) {
    throw apiError_('Bill items are incomplete, so its PDF cannot be refreshed. Ask the manager to check this bill.', 409);
  }
  const invoice = {
    number: invoiceNumber, date: date,
    subtotal: lines.reduce(function (sum, line) { return sum + Math.round(invoicePriceQuantity_(line.quantity, line.measureQuantity, line.unit, line.pricingUnit) * line.unitPrice); }, 0),
    discount: lines.reduce(function (sum, line) { return sum + line.discount; }, 0),
    total: total,
    paymentMode: String(invoiceRow['Payment Mode'] || ''), paid: paid, due: Math.max(0, total - paid),
  };
  const customer = { name: String(invoiceRow['Customer Name'] || ''), phone: String(invoiceRow['Customer Phone'] || '') };
  const pdf = buildInvoicePdf_(invoice, customer, lines);
  Drive.Files.update({ name: invoiceNumber + '.pdf' }, fileId, pdf, { fields: 'id,mimeType,parents,name' });
  const bytes = pdf.getBytes();
  if (bytes.length > 4 * 1024 * 1024) throw apiError_('This bill PDF is too large to share from the phone.', 413);
  return {
    fileName: invoiceNumber + '.pdf',
    mimeType: 'application/pdf',
    data: Utilities.base64Encode(bytes),
  };
}

function generateInvoicePdf_(config, payload) {
  const invoiceNumber = String(payload.invoiceNumber || '').trim();
  if (!invoiceNumber) throw apiError_('Choose a bill before generating its PDF.', 400);
  const invoices = getTable_(config, 'Sales Invoices');
  const invoiceRow = invoices.rows.find(function (row) { return String(row['Invoice #'] || '').trim() === invoiceNumber; });
  if (!invoiceRow) throw apiError_('This bill was not found in Shop. Sync bills and try again.', 404);
  if (String(invoiceRow.Status || '').toUpperCase() === 'VOIDED') throw apiError_('This bill has been cancelled by the administrator.', 409);
  const existingUrl = String(invoiceRow['Invoice File'] || '').trim();
  if (/\/file\/d\/[A-Za-z0-9_-]+/.test(existingUrl) || /[?&]id=[A-Za-z0-9_-]+/.test(existingUrl)) {
    return { pdfUrl: existingUrl, ready: true };
  }

  const total = Number(invoiceRow['Invoice Total'] || 0);
  const payments = readPaymentsTable_(config);
  const paidByInvoice = paymentsByInvoice_(payments);
  const paid = Math.min(total, paidAmountForInvoice_(payments, paidByInvoice, invoiceNumber, invoiceRow, total));
  const dateValue = invoiceRow.Date;
  const date = typeof dateValue === 'number'
    ? Utilities.formatDate(new Date(Date.UTC(1899, 11, 30) + dateValue * 86400000), APP.timeZone, 'yyyy-MM-dd')
    : String(dateValue || '');
  ensureColumn_(config, 'Sales Items', 'Unit');
  ensureColumn_(config, 'Sales Items', 'Pricing Unit');
  ensureColumn_(config, 'Sales Items', 'Measure Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  const inventoryRows = getTable_(config, 'Inventory').rows;
  const itemRows = getTable_(config, 'Sales Items').rows.filter(function (row) {
    return String(row['Invoice #'] || '').trim() === invoiceNumber;
  });
  const lines = itemRows.map(function (row) {
    const quantity = Number(row.Quantity || 0);
    const unitPrice = Number(row['Unit Price'] || 0);
    const discount = Number(row.Discount || 0);
    const product = inventoryRows.find(function (candidate) { return String(candidate.SKU || '') === String(row.SKU || ''); }) || {};
    return {
      sku: String(row.SKU || ''), name: String(row['Product Name'] || product['Product Name'] || ''),
      category: String(row.Category || product.Category || ''), quantity: quantity,
      fractionalQuantity: fractionalQuantityEnabled_(product['Fractional Quantity']),
      unit: normalizedProductUnit_(row.Unit, product['Fractional Quantity']), unitPrice: unitPrice,
      unitCost: 0, discount: discount, measureQuantity: Number(row['Measure Quantity'] || 0),
      pricingUnit: normalizedPricingUnit_(row['Pricing Unit'] || product['Pricing Unit']),
      lineTotal: Number(row['Line Total'] || Math.max(0, invoicePriceQuantity_(quantity, Number(row['Measure Quantity'] || 0), normalizedProductUnit_(row.Unit, product['Fractional Quantity']), normalizedPricingUnit_(row['Pricing Unit'] || product['Pricing Unit']) || productUnit_(product)) * unitPrice - discount)),
    };
  });
  if (total <= 0 || !lines.length || lines.some(function (line) { return !line.name || line.quantity < 0.001 || line.unitPrice <= 0 || line.lineTotal <= 0; })) {
    throw apiError_('The bill is saved, but its PDF details are incomplete. Ask the manager to check the bill.', 409);
  }
  const invoice = {
    number: invoiceNumber, date: date,
    subtotal: lines.reduce(function (sum, line) { return sum + Math.round(invoicePriceQuantity_(line.quantity, line.measureQuantity, line.unit, line.pricingUnit) * line.unitPrice); }, 0),
    discount: lines.reduce(function (sum, line) { return sum + line.discount; }, 0),
    total: total, paymentMode: String(invoiceRow['Payment Mode'] || ''), paid: paid, due: Math.max(0, total - paid),
  };
  const customer = { name: String(invoiceRow['Customer Name'] || ''), phone: String(invoiceRow['Customer Phone'] || '') };
  const pdf = buildInvoicePdf_(invoice, customer, lines);
  let fileId = '';
  try {
    const file = Drive.Files.create({
      name: invoiceNumber + '.pdf', mimeType: 'application/pdf', parents: [config.invoiceFolderId],
    }, pdf, { fields: 'id,webViewLink' });
    fileId = file.id;
    shareInvoice_(file.id, config.authorizedEmails);
    const invoiceFileColumn = invoices.headers.indexOf('Invoice File');
    if (invoiceFileColumn < 0) throw apiError_('The Sales Invoices tab is missing its Invoice File column.', 500);
    const pdfUrl = file.webViewLink || 'https://drive.google.com/file/d/' + file.id + '/view';
    Sheets.Spreadsheets.batchUpdate({ requests: [{
      updateCells: {
        range: { sheetId: invoices.sheetId, startRowIndex: invoiceRow._rowNumber - 1, endRowIndex: invoiceRow._rowNumber, startColumnIndex: invoiceFileColumn, endColumnIndex: invoiceFileColumn + 1 },
        rows: [{ values: [{ userEnteredValue: { stringValue: pdfUrl } }] }], fields: 'userEnteredValue',
      },
    }] }, config.spreadsheetId);
    return { pdfUrl: pdfUrl, ready: true };
  } catch (error) {
    if (fileId) { try { Drive.Files.delete(fileId); } catch (cleanupError) { console.error(cleanupError); } }
    throw error;
  }
}

function customerFinance_(config, phone) {
  const digits = normalizedPhone_(phone);
  if (digits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  const invoices = getTable_(config, 'Sales Invoices').rows.filter(function (row) {
    const total = Number(row['Invoice Total']);
    return normalizedPhone_(row['Customer Phone']) === digits && String(row.Status || '').toUpperCase() !== 'VOIDED' && String(row['Invoice #'] || '').trim() && Number.isFinite(total) && total > 0;
  });
  const payments = readPaymentsTable_(config);
  const paidByInvoice = paymentsByInvoice_(payments);
  const advanceBalance = (payments ? payments.rows : []).reduce(function (sum, row) {
    return !isVoidedPayment_(row) && normalizedPhone_(row['Customer Phone']) === digits &&
      !String(row['Invoice #'] || '').trim() && Number(row.Amount) > 0 ? sum + Number(row.Amount) : sum;
  }, 0);
  let totalPurchases = 0;
  let totalPaid = 0;
  invoices.forEach(function (row) {
    const number = String(row['Invoice #']);
    const total = Number(row['Invoice Total']);
    const paid = paidAmountForInvoice_(payments, paidByInvoice, number, row, total);
    totalPurchases += total;
    totalPaid += Math.min(total, paid);
  });
  const customerId = invoices.length ? String(invoices[0]['Customer ID'] || '') : '';
  const receiptGroups = {};
  (payments ? payments.rows : [])
    .filter(function (row) { return normalizedPhone_(row['Customer Phone']) === digits && Number(row.Amount) > 0; })
    .forEach(function (row) {
      const receiptNumber = String(row['Receipt #'] || row['Payment ID'] || '');
      if (!receiptGroups[receiptNumber]) receiptGroups[receiptNumber] = {
        id: receiptNumber, receiptNumber: receiptNumber, date: String(row.Date || ''),
        invoiceNumbers: [], amount: 0, advanceAmount: 0, method: String(row['Payment Method'] || ''),
        note: String(row.Notes || ''), requestId: String(row['Client Request ID'] || ''),
        voided: false, lastRow: row._rowNumber,
      };
      const receipt = receiptGroups[receiptNumber];
      receipt.voided = receipt.voided || isVoidedPayment_(row);
      const invoiceNumber = String(row['Invoice #'] || '');
      if (invoiceNumber && receipt.invoiceNumbers.indexOf(invoiceNumber) < 0) receipt.invoiceNumbers.push(invoiceNumber);
      if (!invoiceNumber && !isVoidedPayment_(row)) receipt.advanceAmount += Number(row.Amount || 0);
      receipt.amount += Number(row.Amount || 0);
      receipt.lastRow = Math.max(receipt.lastRow, row._rowNumber);
    });
  const recentPayments = Object.keys(receiptGroups).map(function (key) { return receiptGroups[key]; })
    .sort(function (a, b) { return b.lastRow - a.lastRow; })
    .slice(0, 50)
    .map(function (receipt) {
      return {
        id: receipt.id, receiptNumber: receipt.receiptNumber, date: receipt.date,
        invoiceNumber: receipt.invoiceNumbers.join(', '), amount: receipt.amount,
        advanceAmount: receipt.advanceAmount,
        method: receipt.method, note: receipt.note, requestId: receipt.requestId, voided: receipt.voided,
      };
    });
  return {
    summary: {
      totalPurchases: totalPurchases,
      totalPaid: totalPaid,
      currentDue: Math.max(0, totalPurchases - totalPaid),
      advanceBalance: advanceBalance,
      invoiceCount: invoices.length,
      customerId: customerId,
    },
    payments: recentPayments,
  };
}

function listPayments_(config) {
  const payments = readPaymentsTable_(config);
  if (!payments) return [];
  const customersByPhone = {};
  getTable_(config, 'Customers').rows.forEach(function (row) {
    customersByPhone[normalizedPhone_(row.Phone)] = String(row['Customer Name'] || '');
  });
  const groups = {};
  payments.rows.filter(function (row) { return Number(row.Amount) > 0; }).forEach(function (row) {
    const receiptNumber = String(row['Receipt #'] || row['Payment ID'] || '');
    if (!groups[receiptNumber]) groups[receiptNumber] = {
      id: receiptNumber, receiptNumber: receiptNumber, date: String(row.Date || ''),
      customerName: customersByPhone[normalizedPhone_(row['Customer Phone'])] || '',
      customerPhone: String(row['Customer Phone'] || ''), invoiceNumbers: [], amount: 0, advanceAmount: 0,
      method: String(row['Payment Method'] || ''), note: String(row.Notes || ''),
      requestId: String(row['Client Request ID'] || ''), voided: isVoidedPayment_(row), lastRow: row._rowNumber,
    };
    const group = groups[receiptNumber];
    group.voided = group.voided || isVoidedPayment_(row);
    const invoiceNumber = String(row['Invoice #'] || '').trim();
    if (invoiceNumber && group.invoiceNumbers.indexOf(invoiceNumber) < 0) group.invoiceNumbers.push(invoiceNumber);
    if (!invoiceNumber && !isVoidedPayment_(row)) group.advanceAmount += Number(row.Amount || 0);
    group.amount += Number(row.Amount || 0);
    group.lastRow = Math.max(group.lastRow, row._rowNumber);
  });
  return Object.keys(groups).map(function (key) { return groups[key]; })
    .sort(function (a, b) { return b.lastRow - a.lastRow; })
    .slice(0, 200)
    .map(function (group) {
      return {
        id: group.id, receiptNumber: group.receiptNumber, date: group.date,
        customerName: group.customerName, customerPhone: group.customerPhone,
        invoiceNumber: group.invoiceNumbers.join(', '), amount: group.amount,
        advanceAmount: group.advanceAmount,
        method: group.method, note: group.note, requestId: group.requestId, voided: group.voided,
      };
    });
}

function receivePayment_(config, payload, userEmail) {
  const phone = String(payload.phone || '').trim();
  const digits = normalizedPhone_(phone);
  const invoiceNumber = String(payload.invoiceNumber || '').trim();
  const amount = Number(payload.amount);
  const method = String(payload.method || '').trim();
  const requestId = paymentRequestId_(payload.requestId);
  if (digits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  if (!invoiceNumber) throw apiError_('Choose the bill this payment is for.', 400);
  if (!Number.isInteger(amount) || amount < 1) throw apiError_('Enter a whole-number payment amount greater than zero.', 400);
  if (!['Cash', 'UPI', 'Card'].includes(method)) throw apiError_('Choose Cash, UPI, or Card for this payment.', 400);

  const invoices = getTable_(config, 'Sales Invoices');
  const invoice = invoices.rows.find(function (row) {
    return String(row['Invoice #'] || '').trim() === invoiceNumber && String(row.Status || '').toUpperCase() !== 'VOIDED' && normalizedPhone_(row['Customer Phone']) === digits;
  });
  if (!invoice) throw apiError_('That bill was not found for this customer.', 404);
  const total = Number(invoice['Invoice Total']);
  if (!Number.isFinite(total) || total <= 0) throw apiError_('This bill does not have a valid total.', 400);
  const payments = ensurePaymentsTable_(config);
  const previousRows = payments.rows.filter(function (row) { return String(row['Client Request ID'] || '') === requestId; });
  if (previousRows.length) {
    if (previousRows.every(isVoidedPayment_)) throw apiError_('This payment was cancelled by the administrator. Record a new payment if the customer has paid again.', 409);
    const existing = previousRows[0];
    if (String(existing['Invoice #'] || '').trim() !== invoiceNumber || normalizedPhone_(existing['Customer Phone']) !== digits) {
      throw apiError_('This payment reference was already used for a different bill.', 409);
    }
    const recordedAmount = payments.rows.filter(function (row) { return !isVoidedPayment_(row) && String(row['Invoice #'] || '').trim() === invoiceNumber; })
      .reduce(function (sum, row) { return sum + Number(row.Amount || 0); }, 0);
    return {
      payment: {
        id: existing['Payment ID'], receiptNumber: existing['Receipt #'], date: existing.Date,
        invoiceNumber: existing['Invoice #'], amount: Number(existing.Amount || amount),
        method: existing['Payment Method'], note: existing.Notes, requestId: requestId,
      },
      invoiceDue: Math.max(0, total - recordedAmount),
    };
  }
  const invoicePaymentRows = payments.rows.filter(function (row) { return !isVoidedPayment_(row) && String(row['Invoice #'] || '').trim() === invoiceNumber; });
  const recordedAmount = invoicePaymentRows.reduce(function (sum, row) { return sum + Number(row.Amount || 0); }, 0);
  const hasPaymentRows = payments.rows.some(function (row) { return String(row['Invoice #'] || '').trim() === invoiceNumber; });
  const alreadyPaid = hasPaymentRows ? recordedAmount
    : (String(invoice['Payment Mode'] || '').trim() === 'Credit' ? 0 : total);
  const due = Math.max(0, total - alreadyPaid);
  if (due <= 0) throw apiError_('This bill is already paid in full.', 409);
  if (amount > due) throw apiError_('Payment is greater than the current due of ₹' + due + '.', 400);
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss');
  const receiptNumber = newId_('RCPT');
  const payment = {
    'Payment ID': newId_('PAY'), Date: date,
    'Customer ID': String(invoice['Customer ID'] || ''), 'Customer Phone': phone,
    'Invoice #': invoiceNumber, Amount: amount, 'Payment Method': method,
    'Received By': userEmail, Notes: String(payload.note || '').trim(), 'Receipt #': receiptNumber,
    'Client Request ID': requestId, Status: 'ACTIVE',
  };
  writeRecords_(config, [{ title: 'Payments', table: payments, rowNumber: nextFreeRow_(payments), record: payment }], ['Payments']);
  return {
    payment: {
      id: payment['Payment ID'], receiptNumber: receiptNumber, date: date, invoiceNumber: invoiceNumber,
      amount: amount, method: method, note: payment.Notes, requestId: requestId,
    },
    invoiceDue: due - amount,
  };
}

function ensurePaymentsTable_(config) {
  ensurePaymentsSheet_(config);
  return getTable_(config, 'Payments');
}

function ensurePaymentsSheet_(config) {
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  let sheet = spreadsheet.getSheetByName('Payments');
  if (!sheet) sheet = spreadsheet.insertSheet('Payments');
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, APP.sheets.Payments.length).setValues([APP.sheets.Payments]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  } else {
    const width = Math.max(1, sheet.getLastColumn());
    const headers = sheet.getRange(1, 1, 1, width).getDisplayValues()[0];
    APP.sheets.Payments.forEach(function (header) {
      if (headers.indexOf(header) < 0) {
        const nextColumn = sheet.getLastColumn() + 1;
        sheet.getRange(1, nextColumn).setValue(header).setFontWeight('bold');
        headers.push(header);
      }
    });
  }
  return sheet;
}

function readPaymentsTable_(config) {
  const spreadsheet = SpreadsheetApp.openById(config.spreadsheetId);
  return spreadsheet.getSheetByName('Payments') ? ensurePaymentsTable_(config) : null;
}

function receiveCustomerPayment_(config, payload, userEmail) {
  const phone = String(payload.phone || '').trim();
  const digits = normalizedPhone_(phone);
  const amount = Number(payload.amount);
  const method = String(payload.method || '').trim();
  const requestId = paymentRequestId_(payload.requestId);
  if (digits.length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  if (!Number.isInteger(amount) || amount < 1) throw apiError_('Enter a whole-number payment amount greater than zero.', 400);
  if (!['Cash', 'UPI', 'Card'].includes(method)) throw apiError_('Choose Cash, UPI, or Card for this payment.', 400);

  const invoices = getTable_(config, 'Sales Invoices');
  const payments = ensurePaymentsTable_(config);
  const customer = getTable_(config, 'Customers').rows.find(function (row) { return normalizedPhone_(row.Phone) === digits; });
  if (!customer) throw apiError_('Customer not found. Add the customer before recording a payment.', 404);
  const paidByInvoice = paymentsByInvoice_(payments);
  const previousRows = payments.rows.filter(function (row) { return String(row['Client Request ID'] || '') === requestId; });
  if (previousRows.length) {
    if (previousRows.every(isVoidedPayment_)) throw apiError_('This payment was cancelled by the administrator. Record a new payment if the customer has paid again.', 409);
    if (previousRows.some(function (row) { return normalizedPhone_(row['Customer Phone']) !== digits; })) {
      throw apiError_('This payment reference was already used for a different customer.', 409);
    }
    const invoiceMap = {};
    previousRows.forEach(function (row) {
      const number = String(row['Invoice #'] || '').trim();
      if (number) invoiceMap[number] = (invoiceMap[number] || 0) + Number(row.Amount || 0);
    });
    const allocations = Object.keys(invoiceMap).map(function (number) {
      const invoice = invoices.rows.find(function (row) { return String(row['Invoice #'] || '').trim() === number; });
      const total = invoice ? Number(invoice['Invoice Total'] || 0) : 0;
      return { invoiceNumber: number, amount: invoiceMap[number], dueAfter: Math.max(0, total - paidAmountForInvoice_(payments, paidByInvoice, number, invoice, total)) };
    });
    return {
      receiptNumber: previousRows[0]['Receipt #'], requestId: requestId,
      date: previousRows[0].Date, amount: previousRows.reduce(function (sum, row) { return sum + Number(row.Amount || 0); }, 0),
      method: previousRows[0]['Payment Method'], note: previousRows[0].Notes,
      allocations: allocations,
      advanceAmount: previousRows.filter(function (row) { return !String(row['Invoice #'] || '').trim() && !isVoidedPayment_(row); })
        .reduce(function (sum, row) { return sum + Number(row.Amount || 0); }, 0),
    };
  }
  const candidates = invoices.rows
    .filter(function (row) { return String(row.Status || '').toUpperCase() !== 'VOIDED' && normalizedPhone_(row['Customer Phone']) === digits && String(row['Invoice #'] || '').trim(); })
    .map(function (row) {
      const total = Number(row['Invoice Total']);
      if (!Number.isFinite(total) || total <= 0) return null;
      const number = String(row['Invoice #']);
      const rows = payments.rows.filter(function (payment) { return !isVoidedPayment_(payment) && String(payment['Invoice #'] || '').trim() === number; });
      const paid = paidAmountForInvoice_(payments, paidByInvoice, number, row, total);
      return { row: row, number: number, total: total, due: Math.max(0, total - paid) };
    })
    .filter(function (candidate) { return candidate && candidate.due > 0; })
    .sort(function (a, b) { return a.row._rowNumber - b.row._rowNumber; });
  const totalDue = candidates.reduce(function (sum, candidate) { return sum + candidate.due; }, 0);

  const receiptNumber = newId_('RCPT');
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss');
  let remaining = amount;
  const allocations = [];
  candidates.forEach(function (candidate) {
    if (remaining <= 0) return;
    const allocated = Math.min(candidate.due, remaining);
    allocations.push({ candidate: candidate, amount: allocated });
    remaining -= allocated;
  });
  const advanceAmount = remaining;
  const rowCount = allocations.length + (advanceAmount > 0 ? 1 : 0);
  const rows = nextFreeRows_(payments, rowCount);
  const note = String(payload.note || '').trim();
  const plans = allocations.map(function (allocation, index) {
    return {
      title: 'Payments', table: payments, rowNumber: rows[index],
      record: {
        'Payment ID': newId_('PAY'), Date: date,
        'Customer ID': String(allocation.candidate.row['Customer ID'] || ''), 'Customer Phone': phone,
        'Invoice #': allocation.candidate.number, Amount: allocation.amount, 'Payment Method': method,
        'Received By': userEmail, Notes: note || 'Customer payment allocated to oldest due bills', 'Receipt #': receiptNumber,
        'Client Request ID': requestId, Status: 'ACTIVE',
      },
    };
  });
  if (advanceAmount > 0) {
    plans.push({
      title: 'Payments', table: payments, rowNumber: rows[allocations.length],
      record: {
        'Payment ID': newId_('PAY'), Date: date,
        'Customer ID': String(customer['Customer ID'] || ''), 'Customer Phone': phone,
        'Invoice #': '', Amount: advanceAmount, 'Payment Method': method,
        'Received By': userEmail, Notes: note || 'Customer advance credit', 'Receipt #': receiptNumber,
        'Client Request ID': requestId, Status: 'ACTIVE',
      },
    });
  }
  writeRecords_(config, plans, ['Payments']);
  return { receiptNumber: receiptNumber, requestId: requestId, date: date, amount: amount, method: method, note: note, advanceAmount: advanceAmount, allocations: allocations.map(function (allocation) {
    return { invoiceNumber: allocation.candidate.number, amount: allocation.amount, dueAfter: allocation.candidate.due - allocation.amount };
  }) };
}

function paymentRequestId_(value) {
  const requestId = String(value || '').trim() || newId_('CLIENT');
  if (requestId.length > 120) throw apiError_('The payment reference is invalid. Try again.', 400);
  return requestId;
}

function paymentsByInvoice_(table) {
  const result = {};
  (table ? table.rows : []).forEach(function (row) {
    if (isVoidedPayment_(row)) return;
    const invoiceNumber = String(row['Invoice #'] || '').trim();
    const amount = Number(row.Amount);
    if (invoiceNumber && Number.isFinite(amount) && amount > 0) result[invoiceNumber] = (result[invoiceNumber] || 0) + amount;
  });
  return result;
}

function paidAmountForInvoice_(table, paidByInvoice, invoiceNumber, invoiceRow, total) {
  const hasPaymentRows = (table ? table.rows : []).some(function (row) {
    return String(row['Invoice #'] || '').trim() === invoiceNumber;
  });
  if (hasPaymentRows) return Number(paidByInvoice[invoiceNumber] || 0);
  return invoiceRow && String(invoiceRow['Payment Mode'] || '').trim() === 'Credit' ? 0 : total;
}

function createProduct_(config, payload) {
  ensureProductImageColumn_(config);
  ensureColumn_(config, 'Inventory', 'Availability');
  ensureColumn_(config, 'Inventory', 'Fractional Quantity');
  ensureColumn_(config, 'Inventory', 'Unit');
  ensureColumn_(config, 'Inventory', 'Pricing Unit');
  ensureColumn_(config, 'Inventory', 'Size / Variant');
  ensureColumn_(config, 'Inventory', 'Date Added');
  const name = String(payload.name || '').trim();
  const sizeVariant = String(payload.sizeVariant || '').trim();
  const category = String(payload.category || 'Other').trim() || 'Other';
  const sellingPrice = Number(payload.sellingPrice);
  const unitCost = Number(payload.unitCost || 0);
  const stock = Number(payload.openingStock);
  const unit = normalizedProductUnit_(payload.unit, payload.fractionalQuantity);
  const pricingUnit = normalizedPricingUnit_(payload.pricingUnit) || unit;
  const fractionalQuantity = payload.fractionalQuantity === true && unit !== 'PIECE';
  if (!name || name.length > 120 || sizeVariant.length > 80 || !Number.isFinite(sellingPrice) || sellingPrice < 0 || !Number.isFinite(unitCost) || unitCost < 0 || !Number.isFinite(stock) || stock < 0 || stock > 100000 || (!fractionalQuantity && !Number.isInteger(stock)) || (fractionalQuantity && Math.abs(stock * 1000 - Math.round(stock * 1000)) > 0.000001)) {
    throw apiError_('Enter a product name, valid prices, and a valid opening quantity.', 400);
  }
  const table = getTable_(config, 'Inventory');
  const duplicate = table.rows.find(function (row) {
    return String(row['Product Name'] || '').trim().toLowerCase() === name.toLowerCase() &&
      String(row['Size / Variant'] || '').trim().toLowerCase() === sizeVariant.toLowerCase();
  });
  if (duplicate) throw apiError_('That product name and size / variant already exist. SKU: ' + duplicate.SKU, 409);
  const sku = newId_('SKU');
  const dateAdded = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd');
  const imageId = saveProductImage_(String(payload.imageBase64 || ''), sku);
  const rowNumber = nextFreeRow_(table);
  writeRecords_(config, [{
    title: 'Inventory', table: table, rowNumber: rowNumber,
    record: {
      SKU: sku,
      'Product Name': name,
      'Size / Variant': sizeVariant,
      'Date Added': dateAdded,
      Category: category,
      Supplier: String(payload.supplier || '').trim(),
      'Unit Cost': unitCost,
      'Selling Price': sellingPrice,
      'Opening Stock': stock,
      'Fractional Quantity': fractionalQuantity,
      Unit: unit,
      'Pricing Unit': pricingUnit,
      'Reorder Level': Number(payload.reorderLevel || 0),
      'Image ID': imageId,
      Availability: 'Available',
    },
  }], ['Inventory']);
  return { sku: sku, name: name, sizeVariant: sizeVariant, dateAdded: dateAdded, price: sellingPrice, costPrice: unitCost, stock: stock, imageId: imageId, fractionalQuantity: fractionalQuantity, unit: unit, pricingUnit: pricingUnit, qrValue: sku };
}

function normalizedPricingUnit_(value) {
  const unit = String(value || '').trim().toUpperCase();
  if (unit === 'KG' || unit === 'KGS' || unit === 'KILOGRAM' || unit === 'KILOGRAMS') return 'KG';
  if (unit === 'METRE' || unit === 'METER' || unit === 'M') return 'METRE';
  if (unit === 'PIECE' || unit === 'PIECES' || unit === 'PCS' || unit === 'PC' || unit === 'UNIT') return 'PIECE';
  return '';
}

function invoicePriceQuantity_(quantity, measureQuantity, soldUnit, pricingUnit) {
  const sold = normalizedProductUnit_(soldUnit, false);
  const priced = normalizedPricingUnit_(pricingUnit) || sold;
  return priced === sold ? Number(quantity || 0) : Number(measureQuantity || 0);
}

function createInvoice_(config, payload, userEmail) {
  const customerInput = payload.customer || {};
  const phone = String(customerInput.phone || '').trim();
  const name = String(customerInput.name || '').trim();
  const paymentMode = String(payload.paymentMode || '').trim();
  const items = payload.items;
  if (normalizedPhone_(phone).length < 8) throw apiError_('Enter a valid customer phone number.', 400);
  if (!['Cash', 'UPI', 'Card', 'Credit'].includes(paymentMode)) throw apiError_('Choose Cash, UPI, Card, or Credit.', 400);
  if (!Array.isArray(items) || items.length === 0 || items.length > 100) throw apiError_('Add between 1 and 100 clothes before saving the bill.', 400);
  if (items.some(function (item) { return !item || typeof item !== 'object' || !String(item.sku || '').trim(); })) {
    throw apiError_('One or more bill items are missing a cloth code. Remove the incomplete item and add it again.', 400);
  }

  ensurePaymentsSheet_(config);
  const tables = getTables_(config, ['Inventory', 'Customers', 'Sales Invoices', 'Sales Items', 'Payments']);
  const inventory = tables.Inventory;
  const customers = tables.Customers;
  const invoices = tables['Sales Invoices'];
  const salesItems = tables['Sales Items'];
  const paymentTable = tables.Payments;
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
    if (String(product.Availability || 'Available').trim().toLowerCase() === 'unavailable' || String(product.Availability || '').trim().toLowerCase() === 'false') {
      throw apiError_('Cloth ' + String(product['Product Name'] || item.sku) + ' is unavailable. Sync the catalog and remove it from the bill.', 409);
    }
    if (!String(product['Product Name'] || '').trim()) throw apiError_('A selected cloth is missing its name in Inventory. Ask the manager to correct it before billing.', 400);
    const quantity = Number(item.quantity);
    const fractionalAllowed = fractionalQuantityEnabled_(product['Fractional Quantity']);
    const unit = productUnit_(product);
    const pricingUnit = normalizedPricingUnit_(product['Pricing Unit']) || unit;
    if (!Number.isFinite(quantity) || quantity < 0.001 || (!fractionalAllowed && !Number.isInteger(quantity)) || (fractionalAllowed && Math.abs(quantity * 1000 - Math.round(quantity * 1000)) > 0.000001)) {
      throw apiError_((fractionalAllowed ? 'Enter a valid quantity with up to three decimal places for ' : 'Enter a whole-number quantity for ') + product['Product Name'] + '.', 400);
    }
    const stock = Number(product['Current Stock']);
    if (!Number.isFinite(stock) || stock < 0) throw apiError_('Stock information is incomplete for ' + product['Product Name'] + '. Ask the manager to update Inventory.', 400);
    if (quantity > stock) throw apiError_('Only ' + stock + ' units of ' + product['Product Name'] + ' are in stock.', 409);
    const submittedUnitPrice = item.unitPrice;
    const unitPrice = submittedUnitPrice === undefined || submittedUnitPrice === null || submittedUnitPrice === ''
      ? Number(product['Selling Price'])
      : Number(submittedUnitPrice);
    if (!Number.isFinite(unitPrice) || unitPrice <= 0) throw apiError_('A valid selling price is missing for ' + product['Product Name'] + '. Ask the manager to correct it before billing.', 400);
    const unitCostValue = product['Unit Cost'] === '' ? 0 : Number(product['Unit Cost']);
    if (!Number.isFinite(unitCostValue) || unitCostValue < 0) throw apiError_('Cost information is invalid for ' + product['Product Name'] + '. Ask the manager to correct it before billing.', 400);
    const sellAtCost = item.sellAtCost === true;
    if (sellAtCost && Math.abs(unitPrice - unitCostValue) > 0.001) {
      throw apiError_('Cost-price sale must use the exact cost price for ' + product['Product Name'] + '.', 400);
    }
    if (!sellAtCost && unitPrice < unitCostValue) {
      throw apiError_('Unit price cannot be below cost for ' + product['Product Name'] + '. Use the cost-price option to sell at cost.', 400);
    }
    const discount = Number(item.discount || 0);
    const measureQuantity = Number(item.measureQuantity || 0);
    const pricedQuantity = invoicePriceQuantity_(quantity, measureQuantity, unit, pricingUnit);
    if (pricingUnit !== unit && (!Number.isFinite(measureQuantity) || measureQuantity <= 0 || measureQuantity > 100000 || Math.abs(measureQuantity * 1000 - Math.round(measureQuantity * 1000)) > 0.000001)) {
      throw apiError_('Enter the total ' + (pricingUnit === 'KG' ? 'weight in kg' : pricingUnit === 'METRE' ? 'length in metres' : 'priced quantity') + ' for ' + product['Product Name'] + '.', 400);
    }
    if (!Number.isFinite(discount) || discount < 0 || discount > unitPrice * pricedQuantity) throw apiError_('Discount must be between zero and the item total.', 400);
    return {
      sku: String(product.SKU), name: String(product['Product Name']), category: String(product.Category || ''),
      quantity: quantity, measureQuantity: pricingUnit !== unit ? measureQuantity : 0, pricingUnit: pricingUnit,
      fractionalQuantity: fractionalAllowed, unit: unit, unitPrice: unitPrice, unitCost: unitCostValue, discount: discount,
      lineTotal: Math.max(0, Math.round(pricedQuantity * unitPrice - discount)),
    };
  });

  if (resolvedLines.length !== items.length || resolvedLines.some(function (line) {
    return !line.sku || !line.name || !Number.isFinite(line.quantity) || line.quantity < 0.001 ||
      !Number.isFinite(line.unitPrice) || line.unitPrice <= 0 || !Number.isFinite(line.lineTotal) || line.lineTotal <= 0;
  })) throw apiError_('Some bill details are incomplete. Check the cloth, price, and quantity, then try again.', 400);
  const subtotal = resolvedLines.reduce(function (sum, line) { return sum + line.lineTotal; }, 0);
  const discountValue = Number(payload.discountValue || 0);
  if (String(payload.discountType || '').trim().toUpperCase() === 'PERCENT') {
    throw apiError_('Percentage discounts are not supported. Enter the discount as a rupee amount.', 400);
  }
  if (!Number.isFinite(discountValue) || discountValue < 0) {
    throw apiError_('Enter a valid bill discount.', 400);
  }
  const billDiscount = Math.round(discountValue);
  if (billDiscount > subtotal) throw apiError_('Discount cannot be more than the bill subtotal.', 400);
  if (billDiscount > 0 && subtotal > 0) {
    const allocations = resolvedLines.map(function (line, index) {
      const exact = billDiscount * line.lineTotal / subtotal;
      const floor = Math.floor(exact);
      return { index: index, amount: floor, remainder: exact - floor };
    });
    let remaining = billDiscount - allocations.reduce(function (sum, item) { return sum + item.amount; }, 0);
    allocations.slice().sort(function (a, b) { return b.remainder - a.remainder; }).forEach(function (allocation) {
      if (remaining > 0) { allocations[allocation.index].amount += 1; remaining -= 1; }
    });
    allocations.forEach(function (allocation) {
      const line = resolvedLines[allocation.index];
      line.discount += allocation.amount;
      line.lineTotal -= allocation.amount;
    });
  }
  const total = subtotal - billDiscount;
  if (!Number.isFinite(total) || total <= 0) throw apiError_('The bill total must be greater than zero. Check the item prices and discounts.', 400);
  const combinedCost = resolvedLines.reduce(function (sum, line) { return sum + invoicePriceQuantity_(line.quantity, line.measureQuantity, line.unit, line.pricingUnit) * line.unitCost; }, 0);
  if (total + 0.000001 < combinedCost) {
    throw apiError_('Discount is too high. The bill total cannot be below the combined purchase cost of the items.', 400);
  }
  if (!String(customer['Customer Name'] || name || '').trim()) throw apiError_('Enter the customer name before saving this bill.', 400);
  const amountReceived = paymentMode === 'Credit' ? Number(payload.amountReceived || 0) : total;
  const paymentMethod = paymentMode === 'Credit' ? String(payload.paymentMethod || 'Cash').trim() : paymentMode;
  if (!Number.isInteger(amountReceived) || amountReceived < 0 || amountReceived > total) {
    throw apiError_('Amount received must be between ₹0 and the bill total of ₹' + total + '.', 400);
  }
  if (amountReceived > 0 && !['Cash', 'UPI', 'Card'].includes(paymentMethod)) throw apiError_('Choose Cash, UPI, or Card for the amount received.', 400);
  const due = total - amountReceived;
  const savedPaymentMode = due > 0 ? 'Credit' : paymentMethod;
  // Reserve all bill, item, and payment rows before writing the transaction.
  const invoiceRow = nextFreeRow_(invoices);
  const salesRows = nextFreeRows_(salesItems, resolvedLines.length);
  const paymentRow = amountReceived > 0 ? nextFreeRow_(paymentTable) : 0;
  const receiptNumber = amountReceived > 0 ? newId_('RCPT') : '';
  const number = invoiceNumber_();
  const date = Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd');
  const customerSummary = { name: name || String(customer['Customer Name']), phone: phone };
  try {
    plans.push({
      title: 'Sales Invoices', table: invoices, rowNumber: invoiceRow,
      record: {
        'Invoice #': number,
        Date: dateSerial_(date),
        'Customer ID': customer['Customer ID'],
        'Payment Mode': savedPaymentMode,
        'Invoice File': '',
        'Customer Phone': phone,
        Status: 'ACTIVE',
      },
    });
    resolvedLines.forEach(function (line, index) {
      plans.push({
        title: 'Sales Items', table: salesItems, rowNumber: salesRows[index],
        record: { 'Invoice #': number, SKU: line.sku, Quantity: line.quantity, 'Unit Price': line.unitPrice, Discount: line.discount, Status: 'ACTIVE', Unit: line.unit, 'Pricing Unit': line.pricingUnit, 'Measure Quantity': line.measureQuantity || '' },
      });
    });
    if (amountReceived > 0) {
      plans.push({
        title: 'Payments', table: paymentTable, rowNumber: paymentRow,
        record: {
          'Payment ID': newId_('PAY'), Date: Utilities.formatDate(new Date(), APP.timeZone, 'yyyy-MM-dd HH:mm:ss'),
          'Customer ID': String(customer['Customer ID']), 'Customer Phone': phone,
          'Invoice #': number, Amount: amountReceived, 'Payment Method': paymentMethod,
          'Received By': userEmail, Notes: 'Initial payment at bill creation', 'Receipt #': receiptNumber, Status: 'ACTIVE',
        },
      });
    }
    writeRecords_(config, plans, ['Inventory', 'Customers', 'Sales Invoices', 'Sales Items', 'Payments']);
    return { invoiceNumber: number, customer: customerSummary, items: resolvedLines, subtotal: subtotal, discount: billDiscount, total: total, paid: amountReceived, due: due, paymentMode: savedPaymentMode, pdfUrl: '' };
  } catch (error) {
    throw error;
  }
}

function getTable_(config, title) {
  return getTables_(config, [title])[title];
}

/** Read multiple Shop tabs with one metadata request and one Sheets batchGet. */
function getTables_(config, requestedTitles) {
  const titles = Array.from(new Set(requestedTitles));
  titles.forEach(function (title) {
    if (!APP.sheets[title]) throw apiError_('Unsupported sheet tab: ' + title, 500);
    if (title === 'Stock Adjustments') ensureStockAdjustmentsSheet_(config);
    if (title === 'Inventory') {
      ensureStockAdjustmentsSheet_(config);
      ensureColumns_(config, title, ['Availability', 'Fractional Quantity', 'Unit', 'Pricing Unit', 'Size / Variant', 'Date Added']);
    }
    if (title === 'Sales Invoices' || title === 'Sales Items') ensureColumn_(config, title, 'Status');
  });

  const spreadsheet = Sheets.Spreadsheets.get(config.spreadsheetId, { fields: 'sheets(properties(sheetId,title))' });
  const propertiesByTitle = {};
  (spreadsheet.sheets || []).forEach(function (sheet) { propertiesByTitle[sheet.properties.title] = sheet.properties; });
  const ranges = titles.map(function (title) {
    if (!propertiesByTitle[title]) throw apiError_("Required tab '" + title + "' is missing from Shop.", 500);
    return "'" + title.replace(/'/g, "''") + "'!A1:Z" + APP.maxRows;
  });
  const batch = Sheets.Spreadsheets.Values.batchGet(config.spreadsheetId, {
    ranges: ranges,
    valueRenderOption: 'UNFORMATTED_VALUE',
  });
  const result = {};
  titles.forEach(function (title, tableIndex) {
    const values = ((batch.valueRanges || [])[tableIndex] || {}).values || [];
    const headers = (values[0] || []).map(String);
    const missing = APP.sheets[title].filter(function (header) { return headers.indexOf(header) < 0; });
    if (missing.length) throw apiError_('Tab ' + title + ' is missing columns: ' + missing.join(', '), 500);
    const rows = [];
    for (let index = 1; index < APP.maxRows; index += 1) {
      const valuesRow = values[index] || [];
      const row = { _rowNumber: index + 1 };
      headers.forEach(function (header, column) { row[header] = valuesRow[column] === undefined ? '' : valuesRow[column]; });
      if (String(row[APP.primaryKey[title]] || '').trim()) rows.push(row);
    }
    result[title] = {
      sheetId: propertiesByTitle[title].sheetId,
      spreadsheetId: config.spreadsheetId,
      headers: headers,
      rows: rows,
      title: title,
    };
  });
  return result;
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
  // Refill formulas only in rows written by this transaction. Rewriting every
  // formula row on every sale makes Sheets recalculate the whole workbook and
  // needlessly delays the bill response. Existing formulas recalculate from
  // the changed cells automatically.
  const requests = [];
  rowPlans.forEach(function (plan) {
    requests.push.apply(requests, updateRowRequest_(plan.table, plan.rowNumber, plan.record));
    const formulas = formulasForRow_(plan.title, plan.rowNumber);
    Object.keys(formulas).forEach(function (header) {
      const columnIndex = plan.table.headers.indexOf(header);
      if (columnIndex < 0) return;
      requests.push({
        updateCells: {
          range: { sheetId: plan.table.sheetId, startRowIndex: plan.rowNumber - 1, endRowIndex: plan.rowNumber, startColumnIndex: columnIndex, endColumnIndex: columnIndex + 1 },
          rows: [{ values: [{ userEnteredValue: { formulaValue: formulas[header] } }] }],
          fields: 'userEnteredValue',
        },
      });
    });
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
    'Sold Qty': '=IF(A' + row + '="","",SUMIFS(\'Sales Items\'!$F$2:$F$1000,\'Sales Items\'!$C$2:$C$1000,A' + row + ',\'Sales Items\'!$L$2:$L$1000,"<>VOIDED"))',
    'Current Stock': '=IF(A' + row + '="","",G' + row + '+I' + row + '-J' + row + '+SUMIFS(\'Stock Adjustments\'!$D$2:$D$1000,\'Stock Adjustments\'!$C$2:$C$1000,A' + row + '))',
    'Stock Value': '=IF(A' + row + '="","",IF(S' + row + '<>"","",K' + row + '*E' + row + '))',
    'Stock Status': '=IF(A' + row + '="","",IF(K' + row + '<=0,"Out of Stock",IF(K' + row + '<=H' + row + ',"Low Stock","OK")))',
  };
  if (title === 'Customers') return {
    'Total Orders': '=IF(A' + row + '="","",COUNTIFS(\'Sales Invoices\'!$C$2:$C$1000,A' + row + ',\'Sales Invoices\'!$K$2:$K$1000,"<>VOIDED"))',
    'Total Spent': '=IF(A' + row + '="","",SUMIFS(\'Sales Invoices\'!$F$2:$F$1000,\'Sales Invoices\'!$C$2:$C$1000,A' + row + ',\'Sales Invoices\'!$K$2:$K$1000,"<>VOIDED"))',
    'Last Purchase Date': '=IF(A' + row + '="","",IF(COUNTIFS(\'Sales Invoices\'!$C$2:$C$1000,A' + row + ',\'Sales Invoices\'!$K$2:$K$1000,"<>VOIDED")=0,"",MAXIFS(\'Sales Invoices\'!$B$2:$B$1000,\'Sales Invoices\'!$C$2:$C$1000,A' + row + ',\'Sales Invoices\'!$K$2:$K$1000,"<>VOIDED")))',
  };
  if (title === 'Sales Invoices') return {
    'Customer Name': '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Customers!$A$2:$I$1000,2,FALSE),""))',
    Items: '=IF(A' + row + '="","",COUNTIFS(\'Sales Items\'!$A$2:$A$1000,A' + row + ',\'Sales Items\'!$L$2:$L$1000,"<>VOIDED"))',
    'Invoice Total': '=IF(A' + row + '="","",SUMIFS(\'Sales Items\'!$I$2:$I$1000,\'Sales Items\'!$A$2:$A$1000,A' + row + ',\'Sales Items\'!$L$2:$L$1000,"<>VOIDED"))',
    'Gross Profit': '=IF(A' + row + '="","",SUMIFS(\'Sales Items\'!$K$2:$K$1000,\'Sales Items\'!$A$2:$A$1000,A' + row + ',\'Sales Items\'!$L$2:$L$1000,"<>VOIDED"))',
  };
  if (title === 'Sales Items') return {
    Date: '=IF(A' + row + '="","",IFERROR(VLOOKUP(A' + row + ',\'Sales Invoices\'!$A$2:$H$1000,2,FALSE),""))',
    'Product Name': '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,2,FALSE),""))',
    Category: '=IF(C' + row + '="","",IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,3,FALSE),""))',
    'Line Total': '=IF(OR(C' + row + '="",F' + row + '=""),"",ROUND(MAX(0,IF(AND(N' + row + '<>"",N' + row + '<>M' + row + '),O' + row + ',F' + row + ')*G' + row + '-H' + row + '),0))',
    COGS: '=IF(OR(C' + row + '="",F' + row + '=""),"",IF(AND(N' + row + '<>"",N' + row + '<>M' + row + '),O' + row + ',F' + row + ')*IFERROR(VLOOKUP(C' + row + ',Inventory!$A$2:$M$1000,5,FALSE),0))',
    'Gross Profit': '=IF(OR(C' + row + '="",F' + row + '=""),"",I' + row + '-J' + row + ')',
  };
  return {};
}

function buildInvoicePdf_(invoice, customer, lines) {
  const escape = function (value) { return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) { return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]; }); };
  const money = function (value) {
    const parts = Number(value || 0).toFixed(2).split('.');
    const lastThree = parts[0].slice(-3);
    const otherDigits = parts[0].slice(0, -3).replace(/\B(?=(\d{2})+(?!\d))/g, ',');
    return '₹' + (otherDigits ? otherDigits + ',' : '') + lastThree + '.' + parts[1];
  };
  const status = invoice.due > 0 ? 'DUE' : 'PAID';
  const statusColor = invoice.due > 0 ? '#a95b25' : '#32815c';
  const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
  const dateParts = String(invoice.date || '').split('-');
  const dateLabel = dateParts.length === 3 && Number(dateParts[1]) >= 1 && Number(dateParts[1]) <= 12
    ? Number(dateParts[2]) + ' ' + monthNames[Number(dateParts[1]) - 1] + ' ' + dateParts[0]
    : String(invoice.date || '');
  const bodyRows = lines.map(function (line) {
    const unit = normalizedProductUnit_(line.unit, line.fractionalQuantity);
    const quantityUnit = unit === 'KG' ? ' kg' : unit === 'METRE' ? ' m' : ' pcs';
    const pricedMeasure = normalizedPricingUnit_(line.pricingUnit) || unit;
    const quantityLabel = pricedMeasure !== unit ? Number(line.quantity) + quantityUnit + ' · ' + Number(line.measureQuantity) + (pricedMeasure === 'KG' ? ' kg' : pricedMeasure === 'METRE' ? ' m' : ' pcs') : Number(line.quantity) + quantityUnit;
    const priceUnit = pricedMeasure === 'PIECE' ? ' each' : ' / ' + (pricedMeasure === 'KG' ? 'kg' : 'm');
    return '<tr><td class="item">' + escape(line.name) + '</td><td class="num">' + quantityLabel + '</td><td class="num">' + money(line.unitPrice) + priceUnit + '</td><td class="num">' + money(line.lineTotal) + '</td></tr>';
  }).join('');
  const logo = '<svg class="brand-mark" viewBox="0 0 100 74" role="img" aria-label="Avani Suto logo"><path fill="#102f50" d="M8 67 38 8c3-6 10-6 13 0l26 59H60L43 28 24 67Z"/><path fill="#df713f" d="m44 7 8 1 28 59H65L49 32 39 51 31 55Z"/><path fill="#102f50" d="M22 47 63 39l7 12-54 9Z"/><path fill="#df713f" d="M66 58h15l4 8H70Z"/></svg>';
  const html = '<!doctype html><html><head><meta charset="utf-8"><style>' +
    '@page{size:A5 portrait;margin:12mm 14mm 20mm}html{-webkit-print-color-adjust:exact;print-color-adjust:exact}*{box-sizing:border-box}body{font-family:Arial,sans-serif;color:#1d2936;font-size:10pt;margin:0}.invoice{position:relative;z-index:1;min-height:170mm}.top{display:flex;justify-content:space-between;align-items:flex-start;min-height:26mm}.brand{width:42%;text-align:center}.brand-mark{display:block;width:28mm;height:20mm;margin:0 auto 1mm}.brand-name{font-family:Georgia,serif;font-size:13pt;letter-spacing:1.1px;color:#102f50}.number{text-align:right;padding-top:2mm}.number-label{font-size:9pt;font-weight:bold;color:#596574;letter-spacing:.5px}.number-value{font-size:12pt;margin-top:2mm;white-space:nowrap}.title{font-family:Georgia,serif;color:#102f50;font-size:43pt;line-height:1;font-weight:bold;margin:1mm 0 3mm;letter-spacing:.3px}.date{display:flex;gap:10px;font-size:11pt;margin:0 0 6mm}.date b{font-weight:bold}.parties{display:flex;gap:10mm;margin-bottom:5mm}.party{width:50%;line-height:1.3}.party h2{font-size:12pt;margin:0 0 1mm;color:#202a35}.party .name{font-size:11pt;margin:0 0 1mm}.party .details{font-size:10pt;color:#556170;margin:0}.items{width:100%;border-collapse:collapse;table-layout:fixed;margin:0 0 2mm}.items th{background:#e9edef;font-weight:bold;text-align:left;padding:2mm 2.5mm;font-size:10pt}.items td{padding:2.5mm 2.5mm;border-bottom:1px solid #d1d8dd;font-size:10pt;vertical-align:top}.items .item{width:38%;overflow-wrap:anywhere}.items .qty{width:14%}.items .price{width:24%}.items .amount{width:24%}.num{text-align:right;white-space:nowrap}.total-row{display:flex;justify-content:flex-end;align-items:center;gap:12mm;border-bottom:1px solid #cbd3da;padding:1.5mm 2mm;font-size:10.5pt;font-weight:bold}.total-row strong{color:#102f50;font-size:13pt}.total-row.final{padding:2mm 2mm 2.5mm;font-size:13pt}.total-row.final strong{font-size:16pt}.payment{display:flex;justify-content:space-between;align-items:center;position:relative;padding:4mm 0 1mm;min-height:23mm}.payment-details{line-height:1.65;font-size:10.5pt}.payment-details b{display:inline-block;min-width:34mm}.stamp{border:2px solid ' + statusColor + ';border-radius:5px;color:' + statusColor + ';font-size:18pt;font-weight:bold;letter-spacing:1.5px;padding:1mm 5mm;transform:rotate(-10deg);margin-right:3mm}.thanks{font-size:10pt;color:#566372;margin-top:4mm}.footer-wave{position:absolute;left:-14mm;bottom:-20mm;width:148mm;height:25mm;z-index:-1}.items thead{display:table-header-group}.items tr{page-break-inside:avoid;break-inside:avoid}.total-row,.payment,.thanks{page-break-inside:avoid;break-inside:avoid}' +
    '</style></head><body><main class="invoice">' +
    '<header class="top"><div class="brand">' + logo + '<div class="brand-name">AVANI SUTO</div></div><div class="number"><div class="number-label">INVOICE NO.</div><div class="number-value">' + escape(invoice.number) + '</div></div></header>' +
    '<h1 class="title">INVOICE</h1><p class="date"><b>Date</b><span>' + escape(dateLabel) + '</span></p>' +
    '<section class="parties"><div class="party"><h2>Billed to</h2><p class="name">' + escape(customer.name || 'Walk-in customer') + '</p><p class="details">' + escape(customer.phone || '') + '</p></div><div class="party"><h2>From</h2><p class="name"><b>Avani Suto</b></p><p class="details">Main Road, Ward No 11<br>Simri Bakhtiarpur<br>Saharsa-852127, Bihar</p></div></section>' +
    '<table class="items"><thead><tr><th class="item">Item</th><th class="qty num">Quantity</th><th class="price num">Price</th><th class="amount num">Amount</th></tr></thead><tbody>' + bodyRows + '</tbody></table>' +
    (invoice.discount > 0 ? '<div class="total-row"><span>Subtotal</span><strong>' + money(invoice.subtotal) + '</strong></div><div class="total-row"><span>Discount</span><strong>− ' + money(invoice.discount) + '</strong></div>' : '') +
    '<div class="total-row final"><span>Total</span><strong>' + money(invoice.total) + '</strong></div>' +
    '<section class="payment"><div class="payment-details"><div><b>Payment method</b> ' + escape(invoice.paymentMode) + '</div><div><b>Amount paid</b> ' + money(invoice.paid) + '</div><div><b>Balance due</b> ' + money(invoice.due) + '</div></div><div class="stamp">' + status + '</div></section>' +
    '<p class="thanks">Thank you for shopping with Avani Suto.</p><svg class="footer-wave" viewBox="0 0 600 110" preserveAspectRatio="none" aria-hidden="true"><path fill="#e3e8eb" d="M0 45C125 21 208 22 317 44c112 23 191 27 283-18v84H0Z"/><path fill="#102f50" d="M0 81c123-45 262-53 388-38 84 10 149 1 212-43v110H0Z"/></svg></main></body></html>';
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
  return { id: String(row['Customer ID'] || ''), name: String(row['Customer Name'] || ''), phone: String(row.Phone || ''), email: String(row.Email || ''), customerType: String(row['Customer Type'] || 'Retail') };
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

function productDateAdded_(value) {
  if (value instanceof Date) return Utilities.formatDate(value, APP.timeZone, 'yyyy-MM-dd');
  if (typeof value === 'number' && Number.isFinite(value)) {
    const date = new Date(Date.UTC(1899, 11, 30) + Math.floor(value) * 86400000);
    return Utilities.formatDate(date, APP.timeZone, 'yyyy-MM-dd');
  }
  return String(value || '').trim().slice(0, 10);
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
