package org.rapla.plugin.dualisimport;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.MenuElement;

import org.rapla.facade.CalendarModel;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.rapla.gui.toolkit.RaplaMenuItem;

public class DualisImportWizard extends RaplaGUIComponent implements
        IdentifiableMenuEntry, ActionListener {

    Configuration config;

    private DualisImportWizardDialog dlg;
    

    public DualisImportWizard(RaplaContext sm,Configuration config) {
        super(sm);
        this.config = config;
        setChildBundleName(DualisImportPlugin.RESOURCE_FILE);
    }

	 public String getId() {
			return "300_dualisimportWizard";
	}

	public MenuElement getMenuElement() {
		RaplaMenuItem item = new RaplaMenuItem( getId());
		item.setText( getString("reservation.create_with_import_dualis_plugin"));
		item.setIcon( getIcon("icon.new"));
		item.addActionListener( this);
		boolean canCreateReservation = canCreateReservation();
        item.setEnabled( canAllocate() && canCreateReservation);
		return item;
	}
    

	public void actionPerformed(ActionEvent e) {
		Component mainComponent = getMainComponent();
		try
		{
			CalendarModel model = getService(CalendarModel.class);
			DualisImportWizardDialog wizardDialog = getInstance(getContext(), config,mainComponent, false, model);
		    wizardDialog.startNoPack();
		}
		catch (RaplaException ex)
		{
			showException( ex, mainComponent);
		}
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



}
