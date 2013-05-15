package org.rapla.plugin.dualisimport;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.rapla.facade.CalendarModel;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.DialogUI;

public class DualisImportWizardDialog extends DialogUI {
    private static final long serialVersionUID = 1L;
    private DualisImportPanel dualisImportPanel;

    DualisImportWizardDialog(RaplaContext sm, Configuration config,Dialog owner, CalendarModel model) throws RaplaException {
        super(sm, owner);

        dualisImportPanel =  new DualisImportPanel(sm,config, model);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                dualisImportPanel.updateTimeIntervalLabel();
            }
        });
    }

    DualisImportWizardDialog(RaplaContext sm, Configuration config,Frame owner, CalendarModel model) throws RaplaException {
        super(sm, owner);

        dualisImportPanel =  new DualisImportPanel(sm, config,model);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                dualisImportPanel.updateTimeIntervalLabel();

            }
        });
    }

    void init(boolean modal) {
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(dualisImportPanel.getComponent());
        this.pack();
        this.setResizable(true);
        this.setTitle("Veranstaltungen anlegen");
        this.setSize(800, 565);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    @Override
    public void start() {
        if (!isVisible())
            super.start();

    }

}


