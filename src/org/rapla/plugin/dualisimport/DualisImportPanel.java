package org.rapla.plugin.dualisimport;

import org.rapla.components.util.Assert;
import org.rapla.components.util.TimeInterval;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Configuration;
import org.rapla.framework.ConfigurationException;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaWidget;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;


class DualisImportPanel extends RaplaComponent implements RaplaWidget

{
    private JTable table;
    private CSVImport tableContentCSV;
    private String[] headers;
    private String[][] tableContent;
    private JComboBox programOfStudy, faculties, semester, room, course;
    private JPanel contentPane;

    private RaplaAction createReservationAction;
    private CalendarModel model;
    private JLabel dateContainerLabel;
    private I18nBundle i18n;
    private Configuration config;
    private JCheckBox chkAddSelectedResources;
    private JLabel selectedResources;

    public DualisImportPanel(RaplaContext sm, CalendarModel model) throws RaplaException {
        super(sm);
        this.model = model;
        initConfiguration();
        this.i18n = (I18nBundle) getService(I18nBundle.ROLE + "/" + DualisImportPlugin.RESOURCE_FILE);

        contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(11, 10, 17, 11));
        contentPane.add(createHeader(), BorderLayout.NORTH);
        contentPane.add(createContent(), BorderLayout.CENTER);
        contentPane.add(createButtons(), BorderLayout.SOUTH);

        updateComponentStates();


    }

    private JPanel createContent() {
        table = createTable();
        final JScrollPane scrollPane = new JScrollPane(table);
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 11, 0));
        panel.add(scrollPane);
        return panel;
    }

    private Reservation createReservation(String eventPK, String eventTitle, double sollStunden, String kursPK, String[] dozentenPrimaryKeys, final Date start, final Date end) throws RaplaException {
        final String dualisEventT = getDualisSetting(DualisImportPlugin.DUALIS_EVENT_TYPE);
        final String dualisPKAttribute = getDualisSetting(DualisImportPlugin.DUALIS_PK_ATTRIBUTE);
        final String dualisNameAttribute = getDualisSetting(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE);
        final String dualisSollAttribute = getDualisSetting(DualisImportPlugin.DUALIS_SOLL_ATTRIBUTE);
        final String dualisPersonType = getDualisSetting(DualisImportPlugin.DUALIS_PERSON_TYPE);
        final String dualisCourseType = getDualisSetting(DualisImportPlugin.DUALIS_COURSE_TYPE);


        // dynamic type für event bestimmen
        final DynamicType dualisEventType = getClientFacade().getDynamicType(dualisEventT);

        final Reservation reservation;

        // nur wenn das PK attribute vorliegt, dann können wir evt.  bestehende reservations aktualisieren
        if (dualisEventType.getAttribute(dualisPKAttribute) != null) {
            //attribut set filter laden um zu prüfen, ob event schon existiert
            final ClassificationFilter filter = dualisEventType.newClassificationFilter();
            filter.addEqualsRule(dualisPKAttribute, eventPK);

            final Reservation[] existingReservations = getClientFacade().getReservations(
                    getClientFacade().getUser(), null, null,
                    new ClassificationFilter[]{filter});


            //davon sollte eigentlich nur eines existieren
            Assert.isTrue(existingReservations.length <= 1, "There should be in maximum one reservation for PK " + eventPK);

            // neue erzeugen bzw. editierbare alte holen
            if (existingReservations.length == 1) {
                reservation = getClientFacade().edit(existingReservations[0]);
            } else {
                reservation = getClientFacade().newReservation();
            }

            reservation.setClassification(dualisEventType.newClassification());

            // atribute liegt vor, also auf jeden fall anlegen
            reservation.getClassification().setValue(dualisPKAttribute, eventPK);
        } else {
            reservation = getClientFacade().newReservation();
            reservation.setClassification(dualisEventType.newClassification());
        }
        //sollten die beiden attribute nicht existieren, wird ein Fehler geworfen!
        reservation.getClassification().setValue(dualisNameAttribute, eventTitle);
        reservation.getClassification().setValue(dualisSollAttribute, (int) sollStunden);

        // ersten Termin hinzufügen, nur wenn es noch keinen gibt
        if (reservation.getAppointments().length == 0) {

            final Appointment appointment = getClientFacade().newAppointment(start, end);
            reservation.addAppointment(appointment);
        }

        // das hinzufügen von kursen ist optional
        try {
            final DynamicType dualisKursType = getClientFacade().getDynamicType(dualisCourseType);
            Allocatable course = findMatchingAllocatableByPK(dualisKursType, kursPK);
            if (course != null)
                reservation.addAllocatable(course);
        } catch (RaplaException e) {
            getLogger().error("Adding course resource failed", e);
        }

        // das hinzufügen von räumen ist optional
        if (room.getSelectedIndex() > 0) {
            Allocatable raum = ((StringWrapper<Allocatable>) room.getSelectedItem()).forObject;
            if (raum != null)
                reservation.addAllocatable(raum);

        }

        if (chkAddSelectedResources.isSelected()) {
            final Allocatable[] selectedAllocatables = model.getSelectedAllocatables();
            for (Allocatable selectedAllocatable : selectedAllocatables) {
                reservation.addAllocatable(selectedAllocatable);
            }
        }

        // das hinzufügen von personen ist optional
        try {
            final DynamicType dualisLehrPersonalType = getClientFacade().getDynamicType(dualisPersonType);
            //bekannte Dozenten anhängen
            for (String dozentenPk : dozentenPrimaryKeys) {
                Allocatable person = findMatchingAllocatableByPK(dualisLehrPersonalType, dozentenPk);
                if (person != null)
                    reservation.addAllocatable(person);
            }
        } catch (RaplaException e) {
            getLogger().error("Adding person resource failed", e);
        }

        // storing now in database
        getClientFacade().store(reservation);

        getClientFacade().refresh();

        return reservation;
    }

    private String getDualisSetting(String key) throws RaplaException {
        final String result;
        try {
            result = config.getChild(key).getValue();
        } catch (ConfigurationException e) {
            throw new RaplaException("Could not find configured " + key + ". Please run admin setttings first.");
        }
        if (result == null)
            throw new RaplaException("Could not find configured " + key + ". Please run admin setttings first.");
        return result;
    }


    /**
     * get allocatable by dynamic type (defined in admin options) and primary key in csv
     *
     * @param dynamicType dynamictype, e.g. course or person
     * @param pk          primary key value to be matched against DUALIS_PK_ATTTRIBTES value
     * @return
     * @throws RaplaException
     */
    private Allocatable findMatchingAllocatableByPK(DynamicType dynamicType, String pk) throws RaplaException {
        final String dualisPKAttribute = getDualisSetting(DualisImportPlugin.DUALIS_PK_ATTRIBUTE);
        final Allocatable[] allocatables = getClientFacade().getAllocatables(new ClassificationFilter[]{
                dynamicType.newClassificationFilter()
        });
        for (Allocatable allocatable : allocatables) {
            final Attribute dualisPK = allocatable.getClassification().getAttribute(dualisPKAttribute);
            if (dualisPK == null) {
                throw new RaplaException(dynamicType.toString() + " is missing attribute " + dualisPKAttribute);
            }
            final Object value = allocatable.getClassification().getValue(dualisPK);
            if (value != null && value.equals(pk))
            // found matching person
            {
                return allocatable;
            }
        }
        return null;
    }

    public JPanel createHeader() {
        faculties = new JComboBox();
        faculties.setSize(200, 50);
        faculties.setName(i18n.getString("faculty"));
        faculties.addItem(i18n.getString("faculty"));
        programOfStudy = new JComboBox();
        programOfStudy.setSize(200, 50);
        programOfStudy.setName(i18n.getString("course"));
        programOfStudy.addItem(i18n.getString("course"));
        semester = new JComboBox();
        semester.setSize(200, 50);
        semester.setName(i18n.getString("semester"));
        semester.addItem(i18n.getString("semester"));
        course = new JComboBox();
        course.setSize(200, 50);
        course.setName(i18n.getString("class"));
        course.addItem(i18n.getString("class"));

        faculties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (tableContentCSV.getFaculties().contains(faculties.getSelectedItem().toString())) {
                    //todo: fester spalten index?
                    filterColumnByValue(semester.getSelectedItem().toString(), 8);
                }
            }
        });
        programOfStudy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (tableContentCSV.getProgramOfStudy().contains(programOfStudy.getSelectedItem().toString())) {
                    //todo: fester spalten index?
                    filterColumnByValue(semester.getSelectedItem().toString(), 9);
                }
            }
        });
        semester.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (tableContentCSV.getSemester().contains(semester.getSelectedItem().toString())) {
                    //todo: fester spalten index?
                    filterColumnByValue(semester.getSelectedItem().toString(), 11);
                }
            }
        });
        course.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (tableContent != null && tableContentCSV.getCourse().contains(course.getSelectedItem().toString())) {
                    //todo: fester spalten index?
                    filterColumnByValue(course.getSelectedItem().toString(), 7);
                }
            }
        });

        final JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder(i18n.getString("filters")));

        filterPanel.add(Box.createHorizontalBox());
        filterPanel.add(new JLabel(i18n.getString("faculties")));
        filterPanel.add(faculties);
        filterPanel.add(Box.createHorizontalBox());
        filterPanel.add(new JLabel(i18n.getString("course")));
        filterPanel.add(programOfStudy);
        filterPanel.add(Box.createHorizontalBox());
        filterPanel.add(new JLabel(i18n.getString("semester")));
        filterPanel.add(semester);
        filterPanel.add(Box.createHorizontalBox());
        filterPanel.add(new JLabel(i18n.getString("class")));
        filterPanel.add(course);

        chkAddSelectedResources = new JCheckBox(i18n.getString("add_selected_resources"));
        selectedResources = new JLabel();
        final JPanel options = new JPanel(new GridBagLayout());
        options.setBorder(BorderFactory.createTitledBorder(i18n.getString("options")));

        final Insets insets = new Insets(2, 2, 2, 2);
        options.add(chkAddSelectedResources, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 5, 5));
        options.add(new JScrollPane(selectedResources), new GridBagConstraints(2, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        room = new JComboBox();
        room.setSize(200, 50);
        room.setName(i18n.getString("room"));

        options.add(new JLabel(i18n.getString("room")), new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));
        options.add(room, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, insets, 5, 5));


        final JPanel header = new JPanel(new BorderLayout());
        header.add(options, BorderLayout.NORTH);
        header.add(filterPanel);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        return header;


    }

    private void updateSelectedResources() {
        try {
            final String dualisRoomType = getDualisSetting(DualisImportPlugin.DUALIS_ROOM_TYPE);
            DynamicType roomType = getClientFacade().getDynamicType(dualisRoomType);

            final Allocatable[] selectedAllocatables = model.getSelectedAllocatables();
            chkAddSelectedResources.setSelected(selectedAllocatables.length > 0);
            if (selectedAllocatables.length > 0) {
                final StringBuilder text = new StringBuilder();
                for (int i = 0, selectedAllocatablesLength = selectedAllocatables.length; i < selectedAllocatablesLength; i++) {
                    Allocatable selectedAllocatable = selectedAllocatables[i];
                    text.append(selectedAllocatable.getName(Locale.getDefault()));
                    if (i < selectedAllocatablesLength - 1)
                        text.append(", ");
                    if (selectedAllocatable.getClassification().getType().equals(roomType))
                        room.setSelectedItem(new StringWrapper<Allocatable>(selectedAllocatable));
                }
                selectedResources.setText(text.toString());
            }
        } catch (RaplaException e) {
        }
    }

    public JPanel createButtons() throws RaplaException {
        final JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        final JButton csvImport = new JButton();
        csvImport.setText(i18n.getString("import"));
        csvImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                importFromFile();
            }
        });

        final JButton cancel = new JButton();
        cancel.setText(getString("close"));
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((Window) DialogUI.getOwnerWindow(contentPane)).setVisible(false);
            }
        });

        createReservationAction = new RaplaAction(getContext()) {
            public void actionPerformed(ActionEvent e) {
                createReservation();
            }
        };
        createReservationAction.putValue(Action.ACTION_COMMAND_KEY, "createReservation");
        createReservationAction.putValue(Action.NAME, i18n.getString("create.reservations"));

        final JButton createReservation = new JButton(createReservationAction);

        result.add(dateContainerLabel = new JLabel());
        result.add(Box.createHorizontalGlue());

        result.add(csvImport);
        result.add(Box.createHorizontalGlue());
        result.add(createReservation);
        result.add(Box.createHorizontalGlue());
        result.add(cancel);

        result.setBorder(BorderFactory.createEmptyBorder(0, 0, 17, 0));
        return result;
    }

    private void importFromFile() {
        final JFileChooser fc = new JFileChooser(new File("."));
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(new FileNameExtensionFilter(i18n.getString("csv.files"), "csv"));

        fc.showOpenDialog(contentPane);
        final File selFile = fc.getSelectedFile();
        if (selFile != null && selFile.exists() && selFile.canRead()) {
            try {
                final Reader reader = new FileReader(selFile);

                try {

                    tableContentCSV = new CSVImport(reader);

                    headers = new String[tableContentCSV.getEntries()[0].length];
                    tableContent = new String[tableContentCSV.getEntries().length - 1][tableContentCSV.getEntries()[0].length];
                    for (int i = 0; i < tableContentCSV.getEntries()[0].length; i++) {
                        headers[i] = tableContentCSV.getEntries()[0][i];
                    }
                    for (int i = 0; i < tableContentCSV.getEntries().length - 1; i++) {
                        for (int j = 0; j < tableContentCSV.getEntries()[i].length; j++)
                            tableContent[i][j] = tableContentCSV.getEntries()[i + 1][j];
                    }

                    table.setModel(new DefaultTableModel(tableContent, headers));
                    table.revalidate();
                    table.repaint();

                    disableUnusedColumns();

                    final Object[] fac = tableContentCSV.getFaculties().toArray();
                    Arrays.sort(fac);
                    faculties.setModel(new DefaultComboBoxModel(fac));
                    faculties.updateUI();

                    final Object[] pos = tableContentCSV.getProgramOfStudy().toArray();
                    Arrays.sort(pos);
                    programOfStudy.setModel(new DefaultComboBoxModel(pos));
                    programOfStudy.updateUI();

                    final Object[] sem = tableContentCSV.getSemester().toArray();
                    Arrays.sort(sem);
                    semester.setModel(new DefaultComboBoxModel(sem));
                    semester.updateUI();

                    final Object[] cou = tableContentCSV.getCourse().toArray();
                    Arrays.sort(cou);
                    course.setModel(new DefaultComboBoxModel(cou));
                    course.updateUI();

                    updateRooms();


                } finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e) {
                try {
                    DialogUI error = DialogUI.create(getContext(), contentPane, true, "Error", e.getMessage());

                    error.start();

                } catch (RaplaException e1) {

                }

            }

        }
    }

    private void createReservation() {
        final int[] rows = table.getSelectedRows();

        if (rows != null) {
            //todo: internationalization
            final ProgressMonitor pm = new ProgressMonitor(contentPane, "Veranstaltungen werden aktualisiert/hinzugefügt", "", 1, rows.length);

            // start und endzeit fixieren, da der Kalender nach einfügen auf den ersten Tag der View zurückspringt!
            final Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
            Date start = model.getStartDate();
            Date end = model.getEndDate();
            if (!markedIntervals.isEmpty()) {
                final TimeInterval interval = markedIntervals.iterator().next();
                start = interval.getStart();
                end = interval.getEnd();
            }

            Calendar calender = Calendar.getInstance();
            //final Date start = dateContainer.getStartTime() == null ? calender.getTime() : dateContainer.getStartTime();
            start = start == null ? calender.getTime() : start;
            calender.setTime(start);
            calender.add(Calendar.HOUR_OF_DAY, 1);
            end = end == null ? calender.getTime() : end;
            //final Date end = dateContainer.getEndTime() == null ? calender.getTime() : dateContainer.getEndTime();

            final Date startDate = start;
            final Date endDate = end;
            try {

                pm.setMillisToDecideToPopup(0);

                final String username = getClientFacade().getUser().getUsername();

                final SwingWorker<List<Reservation>, Reservation> sw = new SwingWorker<List<Reservation>, Reservation>() {
                    @Override
                    protected List<Reservation> doInBackground() throws Exception {
                        final List<Reservation> result = new ArrayList<Reservation>();
                        for (int i = 0, rowsLength = rows.length; i < rowsLength; i++) {
                            final int row = rows[i];
                            setProgress(i + 1);
                            final String dozentenPK = table.getValueAt(row, DualisImportPlugin.PERSON_PK_COLUMN).toString();
                            final String eventTitle = table.getValueAt(row, DualisImportPlugin.EVENT_TITLE_COLUMN).toString();
                            final String kursPK = table.getValueAt(row, DualisImportPlugin.COURSE_PK_COLUMN).toString();

                            double sollStunden = 0;
                            try {
                                final String value = table.getValueAt(row, DualisImportPlugin.SOLL_COLUMN).toString();
                                sollStunden = Double.parseDouble(value);
                            } catch (NumberFormatException e) {
                                sollStunden = 0;
                            }
                            // todo: internationalization
                            final String eventPK = username + "_" + kursPK + "_" + eventTitle;
                            final Reservation reservation;
                            try {
                                reservation = createReservation(eventPK, eventTitle, sollStunden, kursPK, new String[]{dozentenPK}, startDate, endDate);
                                result.add(reservation);
                                publish(reservation);

                            } catch (RaplaException e) {
                                try {
                                    DialogUI error = DialogUI.create(getContext(), contentPane, true, "Fehler beim Anlegen von Veranstaltung " + eventTitle, e.getMessage());
                                    error.start();
                                } catch (RaplaException e1) {
                                }
                            }


                            if (pm.isCanceled())
                                cancel(true);


                        }
                        return result;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    protected void process(List<Reservation> chunks) {
                        for (Reservation reservation : chunks) {
                            pm.setNote("Veranstaltung " + reservation.getName(Locale.getDefault()) + " wird hinzugefuegt/aktualisiert.");
                        }
                    }

                    @Override
                    protected void done() {
                        pm.close();
                        try {
                            final StringBuilder builder = new StringBuilder();
                            final List<Reservation> reservations = get();
                            final String dualisNameAttribute = getDualisSetting(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE);
                            for (Reservation reservation : reservations) {
                                builder.append(reservation.getClassification().getValue(dualisNameAttribute))
                                        .append(": ")
                                        .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(reservation.getAppointments()[0].getStart()))
                                        .append(" - ")
                                        .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(reservation.getAppointments()[0].getEnd()))
                                        .append("\n");
                            }
                            // todo: internationalization
                            JOptionPane.showMessageDialog(contentPane, "Es wurden " + reservations.size() + " Veranstaltungen erfolgreich hinzugefuegt/aktualisiert.\n\n" + builder);
                        } catch (Exception e) {
                            try {
                                DialogUI error = DialogUI.create(getContext(), contentPane, true, "Error", e.getMessage());
                                error.start();
                            } catch (RaplaException e1) {

                            }//todo: logger

                        }

                    }
                };
                sw.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("progress".equals(evt.getPropertyName())) {
                            pm.setProgress((Integer) evt.getNewValue());
                        }
                    }
                });
                sw.execute();


                /*    for (int i = 0, rowsLength = rows.length; i < rowsLength; i++) {

                        pm.setProgress(i);

                        int row = rows[i];

                        final String dozentenPK = table.getValueAt(row, DualisImportPlugin.PERSON_PK_COLUMN).toString();
                        final String eventTitle = table.getValueAt(row, DualisImportPlugin.EVENT_TITLE_COLUMN).toString();
                        final String kursPK = table.getValueAt(row, DualisImportPlugin.COURSE_PK_COLUMN).toString();
                        double sollStunden;
                        try {
                            final String value = table.getValueAt(row, DualisImportPlugin.SOLL_COLUMN).toString();
                            sollStunden = Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            sollStunden = 0;
                        }
                        // todo: internationalization


                        final Date startDate = start;
                        final Date endDate = end;


                        final String eventPK = getClientFacade().getUser().getUsername() + "_" + kursPK + "_" + eventTitle;
                        SwingUtilities.invokeAndWait(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        final Reservation reservation = createReservation(eventPK, eventTitle, sollStunden, kursPK, new String[]{dozentenPK}, start, end);
                                        reservations.add(reservation);

                                    }
                                }
                        );

                        if (pm.isCanceled())
                            break;

                        successCounter++;
                    }
                */
            } catch (Exception e) {
                try {
                    DialogUI error = DialogUI.create(getContext(), contentPane, true, "Error", e.getMessage());
                    error.start();
                } catch (RaplaException e1) {

                }//todo: logger
            }

            /*  StringBuilder builder = new StringBuilder();


            try {
                final String dualisNameAttribute = getDualisSetting(DualisImportPlugin.DUALIS_NAME_ATTRIBUTE);
                for (Reservation reservation : reservations) {
                    builder.append(reservation.getClassification().getValue(dualisNameAttribute))
                            .append(": ")
                            .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(reservation.getAppointments()[0].getStart()))
                            .append(" - ")
                            .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(reservation.getAppointments()[0].getEnd()))
                            .append("\n");
                }
                // todo: internationalization
                JOptionPane.showMessageDialog(contentPane, "Es wurden " + successCounter + " Veranstaltungen erfolgreich hinzugefügt/aktualisiert.\n\n" + builder);
            } catch (RaplaException e) {
                try {
                    DialogUI error = DialogUI.create(getContext(), contentPane, true, "Error", e.getMessage());
                    error.start();
                } catch (RaplaException e1) {

                }//todo: logger
            }*/


        }
    }


    private void disableUnusedColumns() {
        XTableColumnModel columnModel = new XTableColumnModel();
        table.setColumnModel(columnModel);
        table.createDefaultColumnsFromModel();
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(1), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(2), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(5), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(10), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(12), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(13), false);
    }

    private JTable createTable() {
        final JTable table = new JTable();
        table.setAutoCreateColumnsFromModel(true);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateComponentStates();
                }
            }
        });

        return table;
    }

    void updateComponentStates() {
        createReservationAction.setEnabled(table.getSelectedRows().length > 0);
        updateTimeIntervalLabel();
        updateRooms();
        updateSelectedResources();


    }

    private void updateRooms() {
        try {
            final String dualisRoomType = getDualisSetting(DualisImportPlugin.DUALIS_ROOM_TYPE);
            DynamicType roomType = getClientFacade().getDynamicType(dualisRoomType);
            final ClassificationFilter[] allocatableFilter = model.getAllocatableFilter();
            final ArrayList<ClassificationFilter> filters = new ArrayList<ClassificationFilter>();//Arrays.asList(allocatableFilter));
            filters.add(roomType.newClassificationFilter());
            final Allocatable[] allocatables = getClientFacade().getAllocatables(filters.toArray(new ClassificationFilter[filters.size()]));
            Arrays.sort(allocatables, new Comparator<Allocatable>() {
                public int compare(Allocatable o1, Allocatable o2) {
                    return o1.getName(Locale.getDefault()).compareTo(o2.getName(Locale.getDefault()));
                }
            });
            room.setModel(new DefaultComboBoxModel());
            room.addItem(i18n.getString("room"));


            for (Allocatable allocatable : allocatables) {
                room.addItem(new StringWrapper<Allocatable>(allocatable));
            }

        } catch (RaplaException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void filterColumnByValue(final String value, final int col) {
        final String[][] tempContent = tableContentCSV.trimToColumn(tableContent, value, col);
        final DefaultTableModel dataModel = new DefaultTableModel(tempContent, headers);
        table.setModel(dataModel);
        disableUnusedColumns();
        dataModel.fireTableDataChanged();
    }

    public JComponent getComponent() {
        return contentPane;
    }


    public void updateTimeIntervalLabel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
                Date start = model.getStartDate();
                Date end = model.getEndDate();
                if (!markedIntervals.isEmpty()) {
                    final TimeInterval interval = markedIntervals.iterator().next();
                    start = interval.getStart();
                    end = interval.getEnd();
                }

                dateContainerLabel.setText("Selektierter Zeitraum: " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(start) + " - " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(end));

                updateSelectedResources();
            }
        });
    }

    private void initConfiguration() {
        try {
            final ClientFacade facade = (ClientFacade) getContext().lookup(ClientFacade.ROLE);
            Preferences prefs = facade.getPreferences(null);
            final RaplaConfiguration raplaConfiguration = (RaplaConfiguration) prefs.getEntry("org.rapla.plugin");
            config = raplaConfiguration.find("class", DualisImportPlugin.PLUGIN_CLASS);
        } catch (RaplaException e) {
            getLogger().error("Cannot read plugin configuration");
        }
    }

}

