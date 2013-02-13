package org.rapla.plugin.dualisimport;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

public class DualisImportAdminOption extends DefaultPluginOption implements
        ActionListener {

    private JComboBox comboEventType;
    private JComboBox comboCourseType;
    private JComboBox comboPersonType;
    private JComboBox comboRoomType;
    private JTextField pk;
    private JTextField soll;
    private JComboBox comboName;
    private JButton btnAddNumberAttr;
    private JButton btnAddPrimaryKey;
    private ClientFacade facade;
    private I18nBundle i18n;

    public DualisImportAdminOption(RaplaContext sm) throws RaplaException {
        super(sm);
        facade = this.getClientFacade();
        this.i18n = (I18nBundle) getService(I18nBundle.ROLE + "/" + DualisImportPlugin.RESOURCE_FILE);
    }

    protected JPanel createPanel() throws RaplaException {
        DynamicType[] reservations = this.getClientFacade().getDynamicTypes("reservation");
        comboEventType = new JComboBox();
        for (DynamicType reservation : reservations) {
            comboEventType.addItem(new StringWrapper<DynamicType>(reservation));
        }
        DynamicType[] courses = this.getClientFacade().getDynamicTypes("resource");
        comboCourseType = new JComboBox();
        for (DynamicType course : courses) {
            comboCourseType.addItem(new StringWrapper<DynamicType>(course));
        }
        DynamicType[] persons = this.getClientFacade().getDynamicTypes("person");
        comboPersonType = new JComboBox();
        for (DynamicType person : persons) {
            comboPersonType.addItem(new StringWrapper<DynamicType>(person));
        }
        DynamicType[] rooms = this.getClientFacade().getDynamicTypes("resource");
        comboRoomType = new JComboBox();
        for (DynamicType room : rooms) {
            comboRoomType.addItem(new StringWrapper<DynamicType>(room));
        }
        pk = new JTextField();
        soll = new JTextField();
        comboName = new JComboBox();
        btnAddNumberAttr = new JButton(i18n.getString("create.attribute"));
        btnAddNumberAttr.setEnabled(false);
        btnAddNumberAttr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(getComponent(), i18n.getString("confirm.attribute.add")) == JOptionPane.YES_OPTION) {
                    try {
                        createAttribute(AttributeType.INT,  soll.getText(), i18n.getString("attr-number"), ((StringWrapper<DynamicType>) comboEventType.getSelectedItem()).forObject);
                        updateButtonState();
                    } catch (RaplaException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        btnAddPrimaryKey = new JButton(i18n.getString("create.attribute"));
        btnAddPrimaryKey.setEnabled(true);
        btnAddPrimaryKey.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(getComponent(), i18n.getString("confirm.attribute.add")) == JOptionPane.YES_OPTION) {
                    try {
                        createAttribute(AttributeType.STRING,  pk.getText(), i18n.getString("attr-primary-key"), ((StringWrapper<DynamicType>) comboCourseType.getSelectedItem()).forObject);
                        createAttribute(AttributeType.STRING,  pk.getText(), i18n.getString("attr-primary-key"), ((StringWrapper<DynamicType>) comboPersonType.getSelectedItem()).forObject);
                        createAttribute(AttributeType.STRING,  pk.getText(), i18n.getString("attr-primary-key"), ((StringWrapper<DynamicType>) comboEventType.getSelectedItem()).forObject);
                        updateButtonState();
                    } catch (RaplaException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        });
        final JPanel panel = super.createPanel();

        final JPanel content = new JPanel();
        final GridBagLayout layout = new GridBagLayout();
        content.setLayout(layout);

        final Insets insets = new Insets(2, 2, 2, 2);
        int row = 1;
        content.add(createHeaderLabel("resource-configuration"), new GridBagConstraints(1, row,3,1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));

        row++;
        content.add(new JLabel(i18n.getString("event-type")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(comboEventType, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(new JLabel(i18n.getString("event-type-tooltip")), new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(new JLabel(i18n.getString("course-type")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(comboCourseType, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(new JLabel(i18n.getString("course-type-tooltip")), new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(new JLabel(i18n.getString("room-type")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(comboRoomType, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(new JLabel(i18n.getString("room-type-tooltip")), new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;

        content.add(new JLabel(i18n.getString("person-type")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(comboPersonType, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(new JLabel(i18n.getString("person-type-tooltip")), new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(createHeaderLabel("primarykey-configuration"), new GridBagConstraints(1, row, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(new JLabel(i18n.getString("attr-primary-key")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(pk, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(btnAddPrimaryKey, new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(createHeaderLabel("eventtype-configuration"), new GridBagConstraints(1, row, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(new JLabel(i18n.getString("attr-number")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(soll, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(btnAddNumberAttr, new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        row++;
        content.add(new JLabel(i18n.getString("attr-event-name")), new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(comboName, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        content.add(new JLabel(i18n.getString("attr-event-name-tooltip")), new GridBagConstraints(3, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));

        panel.add(content, BorderLayout.CENTER);

        comboEventType.addActionListener(this);
        comboCourseType.addActionListener(this);
        comboPersonType.addActionListener(this);
        comboName.addActionListener(this);

        final AttributeDocHandler listener = new AttributeDocHandler();
        pk.getDocument().addDocumentListener(listener);
        soll.getDocument().addDocumentListener(listener);

        return panel;
    }

    private JLabel createHeaderLabel(String resourceKey) {
        final String string = "<html>" + i18n.getString(resourceKey) + "</html>";
        return new JLabel(string);
    }

    private void createAttribute(AttributeType type, String attrKey, String name, DynamicType dynamicType) throws RaplaException {

        final Attribute att = getModification().newAttribute(type);
        att.setKey(attrKey);
        att.setAnnotation(AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_NO_VIEW);
        att.getName().setName("en", name);
        att.getName().setName("de", name);
        if (!dynamicType.hasAttribute(att)) {
            final DynamicType edit = facade.edit(dynamicType);
            edit.addAttribute(att);
            facade.store(edit);
        }
    }

    protected void addChildren(DefaultConfiguration newConfig) {

        try {
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_EVENT_TYPE, true).setValue(String.valueOf(((StringWrapper<DynamicType>) comboEventType.getSelectedItem()).forObject.getElementKey()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_COURSE_TYPE, true).setValue(String.valueOf(((StringWrapper<DynamicType>) comboCourseType.getSelectedItem()).forObject.getElementKey()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_ROOM_TYPE, true).setValue(String.valueOf(((StringWrapper<DynamicType>) comboRoomType.getSelectedItem()).forObject.getElementKey()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_PERSON_TYPE, true).setValue(String.valueOf(((StringWrapper<DynamicType>) comboPersonType.getSelectedItem()).forObject.getElementKey()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_PK_ATTRIBUTE, true).setValue(String.valueOf(pk.getText()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_SOLL_ATTRIBUTE, true).setValue(String.valueOf(soll.getText()));
            newConfig.getMutableChild(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE, true).setValue(String.valueOf(((StringWrapper<Attribute>) comboName.getSelectedItem()).forObject.getKey()));
        } catch (ConfigurationException e) {
            getLogger().error("An error has occured saving the Export2iCal Configuration " + e.getMessage());
        }

    }

    protected void readConfig(Configuration config) {
        String eventType = config.getChild(DualisImportPlugin.DUALIS_EVENT_TYPE).getValue(DualisImportPlugin.DUALIS_EVENT_TYPE);
        String courseType = config.getChild(DualisImportPlugin.DUALIS_COURSE_TYPE).getValue(DualisImportPlugin.DUALIS_COURSE_TYPE);
        String roomType = config.getChild(DualisImportPlugin.DUALIS_ROOM_TYPE).getValue(DualisImportPlugin.DUALIS_ROOM_TYPE);
        String personType = config.getChild(DualisImportPlugin.DUALIS_PERSON_TYPE).getValue(DualisImportPlugin.DUALIS_PERSON_TYPE);
        String pkText = config.getChild(DualisImportPlugin.DUALIS_PK_ATTRIBUTE).getValue(DualisImportPlugin.DUALIS_PK_ATTRIBUTE);
        String sollText = config.getChild(DualisImportPlugin.DUALIS_SOLL_ATTRIBUTE).getValue(DualisImportPlugin.DUALIS_SOLL_ATTRIBUTE);
        String nameText = config.getChild(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE).getValue(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE);
        try {
            comboEventType.setSelectedItem(new StringWrapper<DynamicType>(this.getClientFacade().getDynamicType(eventType)));

        } catch (RaplaException e) {
        }
        try {
            comboCourseType.setSelectedItem(new StringWrapper<DynamicType>(this.getClientFacade().getDynamicType(courseType)));
        } catch (RaplaException e) {
        }
        try {
            comboPersonType.setSelectedItem(new StringWrapper<DynamicType>(this.getClientFacade().getDynamicType(personType)));
        } catch (RaplaException e) {
        }
        try {
            comboRoomType.setSelectedItem(new StringWrapper<DynamicType>(this.getClientFacade().getDynamicType(roomType)));
        } catch (RaplaException e) {
        }

        try {
            updateAttributeModel();
            comboName.setSelectedItem(new StringWrapper<Attribute>(this.getClientFacade().getDynamicType(eventType).getAttribute(nameText)));
        } catch (RaplaException e) {
        }
        pk.setText(pkText);
        soll.setText(sollText);
    }

    @Override
    public String getDescriptorClassName() {
        return DualisImportPlugin.class.getName();
    }

    public void actionPerformed(ActionEvent e) {
        updateButtonState();

        comboCourseType.revalidate();
        comboEventType.revalidate();
        comboPersonType.revalidate();
        comboRoomType.revalidate();
        pk.revalidate();
        soll.revalidate();
        comboName.revalidate();
        btnAddPrimaryKey.revalidate();
        btnAddNumberAttr.revalidate();

        if (e.getSource() == comboEventType) {
            updateAttributeModel();
        }
    }

    private void updateAttributeModel() {
        comboName.removeAllItems();
        Attribute[] names = ((StringWrapper<DynamicType>) comboEventType.getSelectedItem()).forObject.getAttributes();
        for (Attribute name : names) {
            if (name.getType().equals(AttributeType.STRING)) {
                comboName.addItem(new StringWrapper<Attribute>(name));
            }
        }
    }

    private void updateButtonState() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final DynamicType eventTypeElementKey = ((StringWrapper<DynamicType>) comboEventType.getSelectedItem()).forObject;
                final DynamicType courseTypeElementKey = ((StringWrapper<DynamicType>) comboCourseType.getSelectedItem()).forObject;
                final DynamicType personTypeElementKey = ((StringWrapper<DynamicType>) comboPersonType.getSelectedItem()).forObject;
                try {
                    btnAddPrimaryKey.setEnabled(!attributeExistsInTypes(
                            pk.getText(), courseTypeElementKey, eventTypeElementKey, personTypeElementKey));
                } catch (RaplaException r) {

                }

                try {
                    btnAddNumberAttr.setEnabled(!attributeExistsInTypes(
                            soll.getText(), eventTypeElementKey));
                } catch (RaplaException r) {

                }
            }
        });

    }

    private boolean attributeExistsInTypes(String attrKey, DynamicType... types) throws RaplaException {
        if (attrKey == null)
            return false;
        boolean result = true;
        for (DynamicType type : types) {
            result = result && type.getAttribute(attrKey) != null;
        }
        return result;
    }

    private class AttributeDocHandler implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            updateButtonState();
        }

        public void removeUpdate(DocumentEvent e) {
            updateButtonState();
        }

        public void changedUpdate(DocumentEvent e) {
            updateButtonState();
        }
    }

    public String getName(Locale locale) {
        return "Dualis Import Plugin";
    }


}
