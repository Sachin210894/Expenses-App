// Deploy as a Web App: Execute as Me; access: Anyone.
const SHEET_NAME = "Transactions";
const SECRET_TOKEN = "REPLACE_WITH_A_LONG_RANDOM_SECRET";

function doPost(e) {
  const p = e.parameter || {};
  if (!p.token || p.token !== SECRET_TOKEN) {
    return ContentService.createTextOutput("unauthorized").setMimeType(ContentService.MimeType.TEXT);
  }
  const amount = String(p.amount || "");
  const direction = String(p.direction || "");
  const date = String(p.date || "");
  if (!amount || !["CREDIT", "DEBIT"].includes(direction) || !date) {
    return ContentService.createTextOutput("invalid data").setMimeType(ContentService.MimeType.TEXT);
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    let sh = ss.getSheetByName(SHEET_NAME);
    if (!sh) {
      sh = ss.insertSheet(SHEET_NAME);
      sh.appendRow(["Transaction Date", "Direction", "Amount", "Unique ID"]);
      sh.setFrozenRows(1);
    }
    const id = String(p.id || "");
    if (!id) throw new Error("Missing ID");
    const last = sh.getLastRow();
    if (last > 1) {
      const found = sh.getRange(2, 4, last - 1, 1).createTextFinder(id).matchEntireCell(true).findNext();
      if (found) return ContentService.createTextOutput("duplicate");
    }
    sh.appendRow([date, direction, Number(amount), id]);
    return ContentService.createTextOutput("ok");
  } finally {
    lock.releaseLock();
  }
}
