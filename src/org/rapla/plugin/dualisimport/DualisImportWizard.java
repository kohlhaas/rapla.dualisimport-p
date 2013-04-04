package org.rapla.plugin.dualisimport;

import java.awt.Component;

import javax.swing.Icon;

import org.rapla.facade.CalendarModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationWizard;

public class DualisImportWizard extends RaplaGUIComponent implements
        ReservationWizard {

    private WizardSequence sequence;

    public DualisImportWizard(RaplaContext sm) throws RaplaException {
        super(sm);
        setChildBundleName(DualisImportPlugin.RESOURCE_FILE);
    }

    public void start(Component owner, CalendarModel model) throws RaplaException {
        sequence = new WizardSequence(getContext());
        sequence.start(owner, model);
    }

    public String toString() {
    	//TODO
        return getString("reservation.create_with_import_dualis_plugin");
    }

    public Icon getIcon() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
