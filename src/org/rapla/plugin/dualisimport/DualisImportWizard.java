package org.rapla.plugin.dualisimport;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;

import javax.swing.Icon;

import org.rapla.facade.CalendarModel;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationWizard;
import org.rapla.gui.toolkit.DialogUI;

public class DualisImportWizard extends RaplaGUIComponent implements
        ReservationWizard {

    Configuration config;

    private DualisImportWizardDialog dlg;
    

    public DualisImportWizard(RaplaContext sm,Configuration config) {
        super(sm);
        this.config = config;
        setChildBundleName(DualisImportPlugin.RESOURCE_FILE);
    }

    public void start(Component owner, CalendarModel model) throws RaplaException {
        DualisImportWizardDialog wizardDialog = getInstance(getContext(), config,owner, false, model);
        wizardDialog.startNoPack();
    }


    public DualisImportWizardDialog getInstance(RaplaContext sm, Configuration config, Component owner, boolean modal, CalendarModel model) throws RaplaException {
        if (dlg == null)
        {
            Component topLevel = DialogUI.getOwnerWindow(owner);
            if (topLevel instanceof Dialog)
                dlg = new DualisImportWizardDialog(sm, config,(Dialog) topLevel, model);
            else
                dlg = new DualisImportWizardDialog(sm, config,(Frame) topLevel, model);

            dlg.init(modal);
        }
        return dlg;
    }


    public String toString() {
        return getString("reservation.create_with_import_dualis_plugin");
    }

    public Icon getIcon() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
