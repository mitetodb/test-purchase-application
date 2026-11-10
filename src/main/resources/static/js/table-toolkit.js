/* ===========================================================
   UNIVERSAL TABLE TOOLKIT
   - Add/Remove Items (Purchase Edit/Add)
   - Sorting
   - Column Filtering
   - DataTables Search
   - CSV Export
   - REAL Excel Export (.xlsx)
   =========================================================== */

/* --------------------------
   ITEM ROW HANDLING (Add/Edit)
-------------------------- */

function addItem() {
    const container = document.getElementById("items-container");
    const items = container.querySelectorAll(".item-block");
    const newIndex = items.length;

    const html = `
        <div class="card mb-2 p-2 item-block">
            <div class="row mb-2">
                <div class="col-md-12">
                    <input name="items[${newIndex}].productUrl"
                           class="form-control"
                           placeholder="Product URL"
                           required />
                </div>
            </div>
            <div class="row">
                <div class="col-md-5">
                    <input name="items[${newIndex}].productName"
                           class="form-control"
                           placeholder="Product name"
                           required />
                </div>
                <div class="col-md-2">
                    <input type="number"
                           name="items[${newIndex}].quantity"
                           class="form-control"
                           placeholder="Qty"
                           min="1"
                           required />
                </div>
                <div class="col-md-3">
                    <input type="number" step="0.01"
                           name="items[${newIndex}].unitPrice"
                           class="form-control"
                           placeholder="Unit price"
                           min="0"
                           required />
                </div>
                <div class="col-md-2">
                    <button type="button" class="btn btn-danger"
                            onclick="removeItem(this)">Remove</button>
                </div>
            </div>
        </div>
    `;

    container.insertAdjacentHTML("beforeend", html);
}

function removeItem(button) {
    button.closest(".item-block").remove();
    reindexItems();
}

function reindexItems() {
    document.querySelectorAll(".item-block").forEach((block, index) => {
        block.querySelectorAll("input").forEach(input => {
            const field = input.name.split(".")[1];
            input.name = `items[${index}].${field}`;
        });
    });
}

/* --------------------------
   CSV EXPORT
-------------------------- */

function exportCSV(tableId, filename = "export.csv") {
    const table = document.getElementById(tableId);
    let csv = [];

    table.querySelectorAll("tr").forEach(row => {
        let cols = [...row.querySelectorAll("th,td")].map(e =>
            `"${e.innerText.replace(/"/g, '""')}"`);
        csv.push(cols.join(","));
    });

    const blob = new Blob([csv.join("\n")], {type: "text/csv;charset=utf-8;"});
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
}

/* --------------------------
   EXCEL EXPORT (.xlsx)
   Using SheetJS
-------------------------- */

function exportExcel(tableId, filename = "export.xlsx") {
    const table = document.getElementById(tableId);
    const workbook = XLSX.utils.table_to_book(table, { sheet: "Sheet1" });
    XLSX.writeFile(workbook, filename);
}

/* --------------------------
   INITIALIZE DATATABLE
-------------------------- */

function dataTableLanguage(tableEl) {
    const data = tableEl.dataset || {};
    const language = {};
    if (data.dtSearch) language.search = data.dtSearch;
    if (data.dtLength) language.lengthMenu = data.dtLength;
    if (data.dtInfo) language.info = data.dtInfo;
    if (data.dtInfoEmpty) language.infoEmpty = data.dtInfoEmpty;
    if (data.dtInfoFiltered) language.infoFiltered = data.dtInfoFiltered;
    if (data.dtZero) language.zeroRecords = data.dtZero;
    if (data.dtEmpty) language.emptyTable = data.dtEmpty;
    if (data.dtPrevious || data.dtNext) {
        language.paginate = {};
        if (data.dtPrevious) language.paginate.previous = data.dtPrevious;
        if (data.dtNext) language.paginate.next = data.dtNext;
    }
    return language;
}

function initDataTable(tableId) {
    const tableEl = document.getElementById(tableId);
    let table = $('#' + tableId).DataTable({
        orderCellsTop: true,
        fixedHeader: true,
        pageLength: 25,
        language: dataTableLanguage(tableEl)
    });

    $('#' + tableId + ' thead').on('keyup change', '.column-filter', function () {
        let colIndex = $(this).closest('th').index();
        table.column(colIndex).search(this.value).draw();
    });
}

/* --------------------------
   ATTACH EXPORT BUTTONS
-------------------------- */

function attachExportButtons(containerId, tableId) {
    const container = document.getElementById(containerId);
    if (!container || container.querySelector("button")) {
        return;
    }
    const excelLabel = container.dataset.excelLabel;
    const csvLabel = container.dataset.csvLabel;
    if (!excelLabel || !csvLabel) {
        return;
    }
    const excel = document.createElement("button");
    excel.type = "button";
    excel.className = "btn btn-sm btn-tk-outline";
    excel.textContent = excelLabel;
    excel.addEventListener("click", function () {
        exportExcel(tableId, tableId + ".xlsx");
    });
    const csv = document.createElement("button");
    csv.type = "button";
    csv.className = "btn btn-sm btn-tk-outline";
    csv.textContent = csvLabel;
    csv.addEventListener("click", function () {
        exportCSV(tableId, tableId + ".csv");
    });
    container.append(excel, csv);
}