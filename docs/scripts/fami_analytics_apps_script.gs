/**
 * =========================================================================
 * FAMI — GOOGLE APPS SCRIPT PARA SEGUIMIENTO MENSUAL DE USO (FIREBASE -> SHEETS)
 * =========================================================================
 * Instrucciones rápidas (1 sola vez):
 * 1. Crea una Hoja de Cálculo nueva en Google Drive llamada:
 *    "Fami — Analítica de Uso Mensual y Adopción"
 * 2. Abre Extensiones > Apps Script y pega este código completo.
 * 3. Ejecuta la función `inicializarHojaFami()` para crear las 3 pestañas
 *    con formato, colores de marca Fami y fórmulas automáticas.
 * 4. Haz clic en Implementar > Nueva implementación > Aplicación web:
 *    - Ejecutar como: Yo
 *    - Quién tiene acceso: Cualquier usuario
 * 5. Copia la URL generada y guárdala en Firebase RTDB en:
 *    `/config/analytics_sheet_webhook`
 * =========================================================================
 */

const FEATURE_COLUMNS = [
  "app_open",
  "tab_inicio",
  "tab_cuentas",
  "tab_tareas",
  "tab_compras",
  "tab_metas",
  "tx_manual",
  "tx_voice_nlp",
  "tx_receipt_scan",
  "wallet_transfer",
  "widget_write",
  "widget_voice",
  "widget_scan",
  "widget_gallery",
  "chore_created",
  "shopping_added",
  "goal_created",
  "note_created",
  "recurring_paid",
  "ai_diagnosis",
  "csv_export",
  "ota_check",
  "theme_dark_enabled",
  "mode_individual_active",
  "mode_familiar_active",
  "family_invite_shared",
  "family_joined_by_code"
];

function inicializarHojaFami() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();

  // 1. Pestaña Resumen_Mensual
  let resumen = ss.getSheetByName("Resumen_Mensual") || ss.getSheets()[0];
  resumen.setName("Resumen_Mensual");
  const headers = ["Mes (YYYY-MM)", "Ultima_Sync", "Total_Eventos", ...FEATURE_COLUMNS];
  resumen.getRange(1, 1, 1, headers.length).setValues([headers]);
  resumen.getRange(1, 1, 1, headers.length)
    .setBackground("#101535")
    .setFontColor("#FFFDF9")
    .setFontWeight("bold");
  resumen.setFrozenRows(1);
  resumen.setFrozenColumns(3);

  // 2. Pestaña Eventos_Detalle
  let detalle = ss.getSheetByName("Eventos_Detalle") || ss.insertSheet("Eventos_Detalle");
  const detHeaders = ["Mes", "Feature_Key", "Categoria", "Conteo_Mensual", "Ultima_Actualizacion"];
  detalle.getRange(1, 1, 1, detHeaders.length).setValues([detHeaders]);
  detalle.getRange(1, 1, 1, detHeaders.length)
    .setBackground("#E54629")
    .setFontColor("#FFFFFF")
    .setFontWeight("bold");
  detalle.setFrozenRows(1);

  // 3. Pestaña Decisiones_Producto
  let decisiones = ss.getSheetByName("Decisiones_Producto") || ss.insertSheet("Decisiones_Producto");
  const decHeaders = ["Funcion", "Modulo", "Uso_Mes_Actual", "Decision (Potenciar/Mantener/Simplificar/Eliminar)", "Candidata_Pro", "Notas"];
  decisiones.getRange(1, 1, 1, decHeaders.length).setValues([decHeaders]);
  decisiones.getRange(1, 1, 1, decHeaders.length)
    .setBackground("#1C1C20")
    .setFontColor("#F3F0E8")
    .setFontWeight("bold");
  decisiones.setFrozenRows(1);

  const catalogo = [
    ["Escáner de Recibos IA", "Finanzas IA", 0, "Potenciar", "Sí (límite mensual gratis / ilimitado Pro)", "Alto valor percibido y costo API Gemini"],
    ["Dictado por Voz / NLP IA", "Finanzas IA", 0, "Potenciar", "Sí (cuota generosa gratis / ilimitado Pro)", "Diferenciador clave en LATAM"],
    ["Widget 1-Toque (Escritorio)", "Retención", 0, "Potenciar", "Gratis (gancho de hábito diario)", "Motor #1 de retención diaria"],
    ["Billeteras Multi-Moneda + BCV", "Finanzas", 0, "Mantener", "Gratis (hasta 4) / Ilimitadas en Pro", "Core del producto"],
    ["Diagnóstico Financiero IA", "Finanzas IA", 0, "Potenciar", "Sí (1/mes gratis, ilimitado Pro)", "Excelente gancho para Fami Pro"],
    ["Exportación CSV / Reportes", "Finanzas", 0, "Mantener", "Sí (Fami Pro)", "Valor para usuarios avanzados"],
    ["Modo Familiar (Invitación)", "Viralidad", 0, "Potenciar", "Plan Familiar Pro (1 pago cubre el hogar)", "Crecimiento orgánico boca a boca"],
    ["Tareas y Compras del Hogar", "Hogar", 0, "Evaluar uso", "Gratis", "Revisar mensualmente si se usan o simplificar"],
    ["Metas de Ahorro y Notas", "Libreta", 0, "Evaluar uso", "Gratis", "Revisar retención mensual"]
  ];
  decisiones.getRange(2, 1, catalogo.length, decHeaders.length).setValues(catalogo);
}

/**
 * Recibe el POST JSON desde Firebase Cloud Function `syncAnalyticsToSheet`
 * Payload esperado: { month: "2026-11", syncedAt: "...", features: { app_open: 120, ... } }
 */
function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents);
    const month = body.month || Utilities.formatDate(new Date(), "UTC", "yyyy-MM");
    const syncedAt = body.syncedAt || new Date().toISOString();
    const features = body.features || {};

    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const resumen = ss.getSheetByName("Resumen_Mensual") || ss.getSheets()[0];
    const detalle = ss.getSheetByName("Eventos_Detalle");

    const counts = FEATURE_COLUMNS.map(k => Number(features[k] || 0));
    const total = counts.reduce((a, b) => a + b, 0);
    const rowData = [month, syncedAt, total, ...counts];

    // Upsert fila del mes en Resumen_Mensual
    const lastRow = resumen.getLastRow();
    let targetRow = -1;
    if (lastRow >= 2) {
      const months = resumen.getRange(2, 1, lastRow - 1, 1).getValues().flat();
      const idx = months.indexOf(month);
      if (idx >= 0) targetRow = idx + 2;
    }
    if (targetRow === -1) {
      targetRow = lastRow + 1;
    }
    resumen.getRange(targetRow, 1, 1, rowData.length).setValues([rowData]);

    // Registrar snapshot en Eventos_Detalle
    if (detalle) {
      const detailRows = Object.keys(features).map(k => [
        month,
        k,
        categorizarFeature(k),
        Number(features[k] || 0),
        syncedAt
      ]);
      if (detailRows.length > 0) {
        detalle.getRange(detalle.getLastRow() + 1, 1, detailRows.length, 5).setValues(detailRows);
      }
    }

    return ContentService
      .createTextOutput(JSON.stringify({ ok: true, month, total }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ ok: false, error: String(err) }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function categorizarFeature(key) {
  if (key.startsWith("widget_")) return "Widget 1-Toque";
  if (key.startsWith("tx_") || key.startsWith("wallet_") || key.includes("csv") || key.includes("ai_")) return "Finanzas & IA";
  if (key.startsWith("tab_")) return "Navegación";
  if (key.startsWith("family_") || key.startsWith("mode_")) return "Crecimiento & Modos";
  return "Hogar & Sistema";
}
