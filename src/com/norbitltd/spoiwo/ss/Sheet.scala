package com.norbitltd.spoiwo.ss

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import com.norbitltd.spoiwo.csv.CSVProperties

object Sheet extends Factory {

  private lazy val defaultName = ""
  private lazy val defaultColumns = Nil
  private lazy val defaultRows = Nil
  private lazy val defaultMergedRegions = Nil
  private lazy val defaultPrintSetup = PrintSetup.Default
  private lazy val defaultHeader = Header.None
  private lazy val defaultFooter = Footer.None
  private lazy val defaultProperties = SheetProperties.Default
  private lazy val defaultMargins = Margins.Default

  val Blank = Sheet()

  def apply(name: String = defaultName,
            columns: List[Column] = defaultColumns,
            rows: List[Row] = defaultRows,
            mergedRegions: List[CellRange] = defaultMergedRegions,
            printSetup: PrintSetup = defaultPrintSetup,
            header: Header = defaultHeader,
            footer: Footer = defaultFooter,
            properties: SheetProperties = defaultProperties,
            margins: Margins = defaultMargins): Sheet =
    Sheet(
      name = wrap(name, defaultName),
      columns = columns,
      rows = rows,
      mergedRegions = mergedRegions,
      printSetup = wrap(printSetup, defaultPrintSetup),
      header = wrap(header, defaultHeader),
      footer = wrap(footer, defaultFooter),
      properties = wrap(properties, defaultProperties),
      margins = wrap(margins, defaultMargins)
    )

  def apply(rows: Row*): Sheet = apply(rows = rows.toList)

  def apply(name : String, row : Row, rows: Row*) : Sheet = apply(name = name, rows = row :: rows.toList)

}

case class Sheet private(
                          name: Option[String],
                          columns: List[Column],
                          rows: List[Row],
                          mergedRegions: List[CellRange],
                          printSetup: Option[PrintSetup],
                          header: Option[Header],
                          footer: Option[Footer],
                          properties: Option[SheetProperties],
                          margins: Option[Margins]) {

  def withSheetName(name: String) =
    copy(name = Option(name))

  def withColumns(columns: List[Column]): Sheet =
    copy(columns = columns)

  def withColumns(columns: Column*): Sheet =
    withColumns(columns.toList)

  def withRows(rows: Iterable[Row]): Sheet =
    copy(rows = rows.toList)

  def withRows(rows: Row*): Sheet =
    withRows(rows)

  def withPrintSetup(printSetup: PrintSetup) =
    copy(printSetup = Option(printSetup))

  def withHeader(header: Header) =
    copy(header = Option(header))

  def withFooter(footer: Footer) =
    copy(footer = Option(footer))

  def withMargins(margins: Margins) =
    copy(margins = Option(margins))

  def convertToCSV(properties : CSVProperties = CSVProperties.Default) : (String, String) = {
    name.getOrElse("") -> rows.map(r => r.convertToCSV(properties)).mkString("\n")
  }


  def convert(workbook: XSSFWorkbook): XSSFSheet = {
    val sheetName = name.getOrElse("Sheet " + (workbook.getNumberOfSheets + 1))
    val sheet = workbook.createSheet(sheetName)

    updateColumnsWithIndexes().foreach( _.applyTo(sheet))
    rows.foreach(row => row.convertToXLSX(sheet))
    mergedRegions.foreach(mergedRegion => sheet.addMergedRegion(mergedRegion.convert()))

    printSetup.foreach(_.applyTo(sheet))
    header.foreach(_.applyTo(sheet))
    footer.foreach(_.applyTo(sheet))
    properties.foreach(_.applyTo(sheet))
    margins.foreach(_.applyTo(sheet))
    sheet
  }

  def saveAsXlsx(fileName: String) {
    Workbook(this).saveAsXlsx(fileName)
  }

  def saveAsCsv(fileName : String, properties : CSVProperties = CSVProperties.Default) {
    Workbook(this).saveAsCsv(fileName, properties)
  }

  private def updateColumnsWithIndexes(): List[Column] = {
    val currentColumnIndexes = columns.map(_.index).flatten.toSet
    if (currentColumnIndexes.isEmpty) {
      columns.zipWithIndex.map {
        case (column, index) => column.withIndex(index)
      }
    } else if (currentColumnIndexes.size == columns.size) {
      columns
    } else {
      throw new IllegalArgumentException(
        "When explicitly specifying column index you are required to provide it " +
          "uniquely for all columns in this sheet definition!")
    }
  }

}
