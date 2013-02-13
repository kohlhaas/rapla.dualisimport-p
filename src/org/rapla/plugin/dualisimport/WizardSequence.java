package org.rapla.plugin.dualisimport;

import java.awt.Component;

import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaGUIComponent;

public class WizardSequence extends RaplaGUIComponent {

    public WizardSequence(RaplaContext sm) throws RaplaException {
        super(sm);
        this.setChildBundleName(DualisImportPlugin.RESOURCE_FILE);
    }

    public void start(Component owner, CalendarModel model) throws RaplaException {
        DualisImportWizardDialog wizardDialog = DualisImportWizardDialog.getInstance(getContext(), owner, false, model);
        wizardDialog.startNoPack();
    }

}

