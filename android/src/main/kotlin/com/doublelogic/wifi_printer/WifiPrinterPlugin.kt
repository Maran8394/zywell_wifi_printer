package com.doublelogic.wifi_printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Base64
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import net.posprinter.posprinterface.IMyBinder
import net.posprinter.posprinterface.ProcessData
import net.posprinter.posprinterface.TaskCallback
import net.posprinter.service.PosprinterService
import net.posprinter.utils.BitmapProcess
import net.posprinter.utils.BitmapToByteData
import net.posprinter.utils.DataForSendToPrinterPos80
import net.posprinter.utils.StringUtils

class WifiPrinterPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel
  private var ISCONNECT = false
  private var context: Context? = null

  companion object {
        var myBinder: IMyBinder? = null
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wifi_printer")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "connectPrinter" -> {
                val ipAddress: String? = call.argument("ip")
                if (ipAddress != null) {
                    connectNet(ipAddress, result)
                } else {
                    result.error("INVALID_ARGUMENT", "IP address is null", null)
                }
            }
            "disconnectPrinter" -> {
                disconnectNet(result)
            }
            "getPrinterStatus" ->{
              getPrinterStatus(result)
            }
            "printRowData" -> {
                printRowData(call, result);
            }
            "printImage" -> {
                printImage(call, result);
            }
            "printText" -> {
                printText(call, result);
            }
            "printColData" -> {
                printColData(call, result);
            }
            "printEmptyLine" -> {
                printEmptyLine(call, result);
            }
            "printNormalText"->{
                printNormalText(call, result)
            }
            "printTitle"->{
                printTitle(call, result)
            }
            "printTwoColumnData" ->{
                printTwoColumnData(call, result)
            }
            "printThreeColumnData" ->{
                printThreeColumnData(call, result)
            }
            
            "cutPaper" -> {
                cutPaper(call, result);
            }
            else -> {
                result.notImplemented()
            }
        }
    }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private val mSerConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        myBinder = service as? IMyBinder
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        myBinder = null 
    }
  }

  private fun connectNet(ip: String, result: Result) {
    val context = this.context 

    if (context != null) {
      val intent = Intent(context, PosprinterService::class.java)
      context.bindService(intent, mSerConnection, Context.BIND_AUTO_CREATE)
      val myBinder = myBinder
      if (myBinder != null) {
        if (ISCONNECT) {
          result.success(mapOf("statusCode" to "00", "message" to "Printer already connected"))
        } else {
            myBinder.ConnectNetPort(ip, 9100, object : TaskCallback {
                override fun OnSucceed() {
                    ISCONNECT = true
                    result.success(mapOf("statusCode" to "00", "message" to "Printer connected successfully"))
                }

                override fun OnFailed() {
                    ISCONNECT = false
                    result.error("CONNECTION_FAILED", "Failed to connect to printer", mapOf("statusCode" to "01", "message" to "Failed to connect to printer"))
                }
            })
        }
      } else {
          result.error("SERVICE_NOT_CONNECTED", "Service not connected to printer", mapOf("statusCode" to "02", "message" to "Service not connected to printer"))
      }    
      
    }else {
        result.error("CONTEXT_NOT_FOUND", "Context is null", mapOf("statusCode" to "03", "message" to "Context is null"))
    }
  }

  private fun disconnectNet(result: Result) {
    val context = this.context

    if (context != null) {
        val myBinder = myBinder
        if (myBinder != null) {
            if (ISCONNECT) {
                myBinder.DisconnectCurrentPort(object : TaskCallback {
                    override fun OnSucceed() {
                        ISCONNECT = false
                        result.success(mapOf("statusCode" to "00", "message" to "Disconnected successfully"))
                    }

                    override fun OnFailed() {
                        ISCONNECT = false
                        result.error("DISCONNECT_FAILED", "Failed to disconnect from printer", mapOf("statusCode" to "01", "message" to "Failed to disconnect"))
                    }
                })
            } else {
                // If already disconnected, return success with appropriate message
                result.success(mapOf("statusCode" to "00", "message" to "Printer already disconnected"))
            }
        } else {
            // Return error when service is not connected
            result.error("SERVICE_NOT_CONNECTED", "Service not connected to printer", mapOf("statusCode" to "02", "message" to "Service not connected to printer"))
        }
    } else {
        // Return error if context is null
        result.error("CONTEXT_NOT_FOUND", "Context is null", mapOf("statusCode" to "03", "message" to "Context is null"))
    }
  }

  private fun getPrinterStatus(result: Result){
    if(ISCONNECT){
      val printerStatus = myBinder?.GetPrinterStatus().toString()
      result.success(mapOf<String, Any>("statusCode" to "00", "message" to "Printer status retrieved successfully", "status" to printerStatus))
    }else{
      result.success(mapOf<String, Any>("statusCode" to "01", "message" to "Please connect to the printer"))
    }
  }


    private fun printCut() {
        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {}
                override fun OnFailed() {}
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos80.initializePrinter())
                //directly cut
                //                    list.add(DataForSendToPrinterPos80.selectCutPagerModerAndCutPager(0));
                //feed and cut
                list.add(DataForSendToPrinterPos80.selectCutPagerModerAndCutPager(0x42, 0x66))
                list
            })
        }
    }

    private fun printSample(call: MethodCall, result: MethodChannel.Result){
        if(ISCONNECT){
            myBinder?.WriteDataByUSB(object : TaskCallback{
                override fun OnSucceed() {
                    Log.e("OnSucceed: ", "")

                }
                override fun OnFailed() {
                    Log.e("OnFailed: ", "")

                }
            },
                ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(DataForSendToPrinterPos80.selectCharacterSize(17))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())

                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60, 0))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100, 1))
                    list.add(StringUtils.strTobytes("printer"))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    printCut()
                    list
                }, 6000)
        }
    }

    private fun printBitmap(call: MethodCall, result: MethodChannel.Result) {
        val base64Image = call.argument<String>("image")

        if (base64Image == null) {
            result.error("INVALID_IMAGE", "Image data is missing or invalid", null)
            return
        }
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            val compressedBitmap = BitmapProcess.compressBmpByYourWidth(bitmap, 576)
            if (ISCONNECT) {
                myBinder?.WriteSendData(object : TaskCallback {
                    override fun OnSucceed() {
                        result.success(mapOf("statusCode" to "00", "message" to "Bitmap printed successfully"))
                    }
                    override fun OnFailed() {
                        result.error(
                            "PRINT_FAILED",
                            "Failed to print bitmap",
                            mapOf("statusCode" to "01", "message" to "Failed to print bitmap")
                        )
                    }
                }, ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    list.add(DataForSendToPrinterPos80.initializePrinter())

                    // Process the bitmap into smaller segments
                    val blist: List<Bitmap?> = BitmapProcess.cutBitmap(150, compressedBitmap)
                    for (bmp in blist) {
                        list.add(
                            DataForSendToPrinterPos80.printRasterBmp(
                                0,
                                bmp,
                                BitmapToByteData.BmpType.Dithering,
                                BitmapToByteData.AlignType.Center,
                                576
                            )
                        )
                    }
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    printCut()
                    list
                })
            } else {
                result.error(
                    "PRINTER_NOT_CONNECTED",
                    "Printer is not connected",
                    mapOf("statusCode" to "02", "message" to "Printer is not connected")
                )
            }
        } catch (e: Exception) {
            result.error("DECODE_FAILED", "Failed to decode image: ${e.message}", null)
        }
    }

    private fun printImage(call: MethodCall, result: MethodChannel.Result) {
    val base64Image = call.argument<String>("image")
    if (base64Image.isNullOrEmpty()) {
        result.error("INVALID_IMAGE", "Image data is missing or invalid", null)
        return
    }

    try {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        val compressedBitmap = BitmapProcess.compressBmpByYourWidth(bitmap, 576)

        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {
                    result.success(mapOf("statusCode" to "00", "message" to "Image printed successfully"))
                }

                override fun OnFailed() {
                    result.error("PRINT_FAILED", "Failed to print image", null)
                }
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos80.initializePrinter())

                // Process and print the bitmap
                val blist: List<Bitmap?> = BitmapProcess.cutBitmap(150, compressedBitmap)
                for (bmp in blist) {
                    list.add(
                        DataForSendToPrinterPos80.printRasterBmp(
                            0,
                            bmp,
                            BitmapToByteData.BmpType.Dithering,
                            BitmapToByteData.AlignType.Center,
                            576
                        )
                    )
                }
                list.add(DataForSendToPrinterPos80.printAndFeedLine())
                list
            })
        } else {
            result.error("PRINTER_NOT_CONNECTED", "Printer is not connected", null)
        }
    } catch (e: Exception) {
        result.error("DECODE_FAILED", "Failed to decode image: ${e.message}", null)
    }
}

    private fun printRowData(call: MethodCall, result: MethodChannel.Result) {
    val rowData = call.argument<Map<String, String>>("rowData")
    val fontSize = call.argument<Int>("fontSize") ?: 10
    if (rowData.isNullOrEmpty()) {
        result.error("INVALID_DATA", "Row data is empty or null", null)
        return
    }

    if (ISCONNECT) {
        myBinder?.WriteSendData(object : TaskCallback {
            override fun OnSucceed() {
                result.success(mapOf("statusCode" to "00", "message" to "Row data printed successfully"))
            }

            override fun OnFailed() {
                result.error("PRINT_FAILED", "Failed to print row data", null)
            }
        }, ProcessData {
            val list: MutableList<ByteArray> = ArrayList()
            list.add(DataForSendToPrinterPos80.initializePrinter())

            val no = rowData["no"] ?: ""
            val serviceName = rowData["service_name"] ?: ""
            val amount = rowData["amount"] ?: ""

            val formattedLine = String.format("%-5s%-30s%10s", no, serviceName, amount)
            list.add(StringUtils.strTobytes(formattedLine))
            list.add(DataForSendToPrinterPos80.printAndFeedLine())
            list
        })
    } else {
        result.error("PRINTER_NOT_CONNECTED", "Printer is not connected", null)
    }
}

    private fun printTwoColumnData(call: MethodCall, result: MethodChannel.Result) {
        val rowData = call.argument<Map<String, String>>("rowData")
        if (rowData.isNullOrEmpty()) {
            result.error("INVALID_DATA", "Row data is empty or null", null)
            return
        }

        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {
                    result.success(mapOf("statusCode" to "00", "message" to "2-column data printed successfully"))
                }

                override fun OnFailed() {
                    result.error("PRINT_FAILED", "Failed to print 2-column data", null)
                }
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos80.initializePrinter())

                // Retrieve column data
                val leftColumn = rowData["left"] ?: ""
                val rightColumn = rowData["right"] ?: ""

                // Printer's maximum character width (adjust as per your printer's settings)
                val maxWidth = 40
                val spacing = 2 // Fixed spacing between columns for tighter alignment

                // Calculate dynamic column widths
                val leftColumnWidth = (maxWidth - spacing - rightColumn.length).coerceAtLeast(0)
                val formattedLine = String.format(
                    "%-${leftColumnWidth}s%s",
                    leftColumn,
                    rightColumn
                )

                // Add center alignment
                val totalPadding = (maxWidth - formattedLine.length).coerceAtLeast(0)
                val leftPadding = totalPadding / 2
                val centeredLine = " ".repeat(leftPadding) + formattedLine

                list.add(StringUtils.strTobytes(centeredLine)) // Add the formatted and centered line
                list.add(DataForSendToPrinterPos80.printAndFeedLine()) // Feed to the next line
                list
            })
        } else {
            result.error("PRINTER_NOT_CONNECTED", "Printer is not connected", null)
        }
    }

    private fun printThreeColumnData(call: MethodCall, result: MethodChannel.Result) {
        val rowData = call.argument<Map<String, String>>("rowData")
        if (rowData.isNullOrEmpty()) {
            result.error("INVALID_DATA", "Row data is empty or null", null)
            return
        }

        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {
                    result.success(mapOf(
                        "statusCode" to "00",
                        "message" to "3-column data printed successfully"
                    ))
                }

                override fun OnFailed() {
                    result.error("PRINT_FAILED", "Failed to print 3-column data", null)
                }
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos80.initializePrinter())

                // Retrieve column data
                val leftColumn = rowData["left"] ?: ""
                val middleColumn = rowData["middle"] ?: ""
                val rightColumn = rowData["right"] ?: ""

                // Printer's maximum character width per line
                val maxWidth = 50
                val spacing = 2 // Space between columns

                // Adjusted column width distribution
                val availableWidth = maxWidth - (spacing * 2) // 36 characters available
                val middleColumnWidth = 5 // Fixed width for quantity
                val leftColumnWidth = (availableWidth * 0.68).toInt() // 65% for product name
                val rightColumnWidth = availableWidth - leftColumnWidth - middleColumnWidth // Remaining for price

                // Debugging output
                println("availableWidth: $availableWidth")
                println("leftColumnWidth: $leftColumnWidth")
                println("middleColumnWidth: $middleColumnWidth")
                println("rightColumnWidth: $rightColumnWidth")

                // Split product name into multiple lines if too long
                val productNameLines = leftColumn.chunked(leftColumnWidth) // Break into chunks

                // Print product name in multiple lines if needed
                for ((index, line) in productNameLines.withIndex()) {
                    val qty = if (index == 0) middleColumn.take(middleColumnWidth) else "" // Print qty only on first line
                    val price = if (index == 0) rightColumn.take(rightColumnWidth) else "" // Print price only on first line
                    
                    val formattedLine = String.format(
                        "%-${leftColumnWidth}s %-${middleColumnWidth}s %${rightColumnWidth}s",
                        line,
                        qty,
                        price
                    )
                    
                    list.add(StringUtils.strTobytes(formattedLine))
                }

                // Add print and feed line command
                list.add(DataForSendToPrinterPos80.printAndFeedLine())
                list
            })
        } else {
            result.error("PRINTER_NOT_CONNECTED", "Printer is not connected", null)
        }
    }


    private fun printEmptyLine(call: MethodCall, result: MethodChannel.Result){
        val lineCount = call.argument<Int>("lineCount") ?: 1
        if(ISCONNECT){
            myBinder?.WriteDataByUSB(object : TaskCallback{
                override fun OnSucceed() {
                    Log.e("OnSucceed: ", "")

                }
                override fun OnFailed() {
                    Log.e("OnFailed: ", "")

                }
            },
                ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    for (i in 1..lineCount) {
                        list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    }
                    list
                }, 6000)
        }
    }

    private fun printText(call: MethodCall, result: MethodChannel.Result){
    val text = call.argument<String>("text")
    val fontSize = call.argument<Int>("fontSize")
    val isBold = call.argument<Int>("isBold") ?:0

        if(ISCONNECT){
            myBinder?.WriteDataByUSB(object : TaskCallback{
                override fun OnSucceed() {
                    Log.e("OnSucceed: ", "")

                }
                override fun OnFailed() {
                    Log.e("OnFailed: ", "")

                }
            },
                ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(isBold))
                    if (fontSize!=null || fontSize!=0){
                        list.add(DataForSendToPrinterPos80.selectCharacterSize(fontSize?:1))
                    }
                    list.add(DataForSendToPrinterPos80.selectAlignment(1))
                    list.add(StringUtils.strTobytes(text))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    list
                }, 6000)
        }
    }

    private fun printNormalText(call: MethodCall, result: MethodChannel.Result){
        val text = call.argument<String>("text")
        val align = call.argument<Int>("align") ?: 0


        if(ISCONNECT){
            myBinder?.WriteDataByUSB(object : TaskCallback{
                override fun OnSucceed() {
                    Log.e("OnSucceed: ", "")

                }
                override fun OnFailed() {
                    Log.e("OnFailed: ", "")

                }
            },
                ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.selectAlignment(align))
                    list.add(StringUtils.strTobytes(text))
//                    list.add(DataForSendToPrinterPos80.printAndFeedForward(10))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    list
                }, 6000)
        }
    }

    private fun printColData(call: MethodCall, result: MethodChannel.Result){
        val text1 = call.argument<String>("text1")
        val text2 = call.argument<String>("text2")
        val leftMargin = call.argument<Int>("leftMargin")?:50
        val leftAlign = call.argument<Int>("leftMargin")?:0
        val rightMargin = call.argument<Int>("rightMargin")?:80
        val rightAlign = call.argument<Int>("rightAlign")?:1
        val leftBold = call.argument<Int>("leftBold")?:0
        val rightBold = call.argument<Int>("rightBold")?:0


        if(ISCONNECT){
            myBinder?.WriteDataByUSB(object : TaskCallback{
                override fun OnSucceed() {
                    Log.e("OnSucceed: ", "")

                }
                override fun OnFailed() {
                    Log.e("OnFailed: ", "")

                }
            },
                ProcessData {
                    val list: MutableList<ByteArray> = ArrayList()
                    list.add(DataForSendToPrinterPos80.initializePrinter())
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(leftMargin, leftAlign))
                    list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(leftBold))
                    list.add(DataForSendToPrinterPos80.selectFont(10))
                    list.add(StringUtils.strTobytes(text1))
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(rightMargin, rightAlign))
                    list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(rightBold))
                    list.add(StringUtils.strTobytes(text2))
                    list.add(DataForSendToPrinterPos80.printAndFeedLine())
                    list
                }, 6000)
        }
    }

    private fun printTitle(call: MethodCall, result: MethodChannel.Result) {
        val text = call.argument<String>("text")
        val align = call.argument<Int>("align") ?: 0
        val fontSize = call.argument<Int>("fontSize") ?: 17
        val bold = call.argument<Int>("bold")?:0

    if (ISCONNECT) {
        myBinder?.WriteSendData(object : TaskCallback {
            override fun OnSucceed() {}
            override fun OnFailed() {}
        }, ProcessData {
            val list: MutableList<ByteArray> = ArrayList()
            list.add(DataForSendToPrinterPos80.initializePrinter())
            list.add(DataForSendToPrinterPos80.selectAlignment(align))
            list.add(DataForSendToPrinterPos80.selectCharacterSize(fontSize))
            list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(bold))
            list.add(StringUtils.strTobytes(text))
            list.add(DataForSendToPrinterPos80.printAndFeedLine())
            list
        })
    }

    }

    private fun cutPaper(call: MethodCall, result: MethodChannel.Result) {
        if (ISCONNECT) {
            myBinder?.WriteSendData(object : TaskCallback {
                override fun OnSucceed() {}
                override fun OnFailed() {}
            }, ProcessData {
                val list: MutableList<ByteArray> = ArrayList()
                list.add(DataForSendToPrinterPos80.printAndFeedForward(3))
                list.add(DataForSendToPrinterPos80.selectCutPagerModerAndCutPager(0x42, 0x66)) // Feed and cut
                list
            })
        }

    }





}