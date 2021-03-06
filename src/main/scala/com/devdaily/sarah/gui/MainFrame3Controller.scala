package com.devdaily.sarah.gui

import com.devdaily.sarah.Sarah
import javax.swing.SwingUtilities
import com.devdaily.sarah.plugins.PluginUtils
import com.devdaily.sarah.actors.Brain
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent
import javax.swing.JOptionPane
import javax.swing.Timer
import grizzled.slf4j.Logging
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

class MainFrame3Controller(sarah: Sarah)
extends BaseMainFrameController 
with Logging {

    // needed so i don't send the text to sarah until the 'insert' is complete
    var lastChange = System.currentTimeMillis

    val mainFrame = new MainFrame3
    mainFrame.setPreferredSize(getDesiredFrameSize)
    mainFrame.pack
    mainFrame.setLocationRelativeTo(null)
    mainFrame.setTitle("Sarah")

    configureMainFrameSoSpeechRecognitionStartsOnWindowFocus
    
    val inputWidget = mainFrame.inputField
    val outputArea = mainFrame.outputArea

    /**
     * Timer code
     * ----------
     */
    val waitTime = 1500
    var theTextFieldSeemsToBeChanging = false
    var timer = new Timer(waitTime, null)  // dangerous null use, but new timer is always created in the DocumentListener

    /**
     * this is (was?) needed to re-display the window. after (a) it is hidden with Cmd-H and
     * then (b) re-displayed with Cmd-Tab. without this, the jframe is not visible.
     */
    mainFrame.addComponentListener(new ComponentAdapter {
        override def componentShown(e: ComponentEvent) {
            mainFrame.setVisible(true)
        }
    })

    // timer doc: http://docs.oracle.com/javase/tutorial/uiswing/misc/timer.html
    // this is triggered when the timer goes off
    val textFieldListener = new ActionListener {
        def actionPerformed(ae: ActionEvent) {
            logger.info("*** got text input ***")
            logger.info("*** CALLING BRAIN WITH TEXT = " + inputWidget.getText)
            theTextFieldSeemsToBeChanging = false
            timer.stop
            sarah.sendPhraseToBrain(inputWidget.getText)
        }
    }

    inputWidget.getDocument.addDocumentListener(new DocumentListener() {
        def insertUpdate(e: DocumentEvent) {
            if (theTextFieldSeemsToBeChanging) {
                timer.restart
                logger.info("TEXTFIELD IS CHANGING, TEXT: " + inputWidget.getText)
            } else {
                // textfield hasn't changed in a while, and just received its first insert event
                logger.info("STARTING NEW TIMER, TEXT = " + inputWidget.getText)
                timer = new Timer(waitTime, textFieldListener)
                timer.setRepeats(false)
                timer.setCoalesce(true)
                timer.start
                theTextFieldSeemsToBeChanging = true
            }
        }
        def changedUpdate(e: DocumentEvent) {}
        def removeUpdate(e: DocumentEvent) {}
    })

    // this might be necessary later
    mainFrame.setFocusInTextField


    /* end constructor area */
    
    def setStatusIndicator(status: StatusIndicator) {
        mainFrame.setStatusIndicator(status)
    }

    def clearInputWidget {
        SwingUtils.invokeLater(inputWidget.setText(""))
    }
    
    private def configureMainFrameSoSpeechRecognitionStartsOnWindowFocus {
        val windowAdapter = new WindowAdapter {
            override def windowGainedFocus(e: WindowEvent) {
                sarah.handleWindowGainedFocusEvent
            }
        }
        mainFrame.addWindowFocusListener(windowAdapter)
    }
    
    def showOutput(text: String) {
        outputArea.setText(text)
    }
    
    def clearOutput {
        outputArea.setText("")
    }
    
    def getMainFrame = mainFrame

    def getTextField = mainFrame.inputField

    def updateUIBasedOnStates {}

    def updateUISpokenTextIsBeingAnalyzed {
        setStatusIndicator(YellowLight)
    }

    // this is running on the swing thread (edt), so 'sleep' isn't really a good idea
    def updateUIWeCouldNotHandleInputText {
        setStatusIndicator(RedLight)
        PluginUtils.runInThread({
            Thread.sleep(1000)
            setStatusIndicator(NeutralLight)
        })
    }

    def updateUISpeakingHasStarted {
        //inputWidget.setText("")
        setStatusIndicator(GreenLight)
    }
    
    def updateUISpeakingHasEnded {
        inputWidget.setText("")
        setStatusIndicator(NeutralLight)
    }
    
    private def getDesiredFrameSize: Dimension = {
        val screenSize = Toolkit.getDefaultToolkit.getScreenSize
        val height = screenSize.height * 2 / 3
        val width = (1.6 * height).asInstanceOf[Int]
        new Dimension(width, height)
    }
    

}









