package org.rapla.plugin.dualisimport;

import org.rapla.client.ClientServiceContainer;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.TypedComponentRole;
import org.rapla.plugin.RaplaClientExtensionPoints;

public class DualisImportPlugin implements PluginDescriptor<ClientServiceContainer> {
    public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>( DualisImportPlugin.class.getPackage().getName() + ".DualisImportResources");
    public static final boolean ENABLE_BY_DEFAULT = true;

    // alle veranstaltungstypen auflisten -> ergebnis ist key fuer d<ynamic typ(Combobox)
    public static final String DUALIS_EVENT_TYPE = "dualisEventType";
    // Resourcentypen auflisten                        (Combobox)
    public static final String DUALIS_COURSE_TYPE = "dualisCourseType";
    // Personenressourcentypen auflisten und auswaehlen (Combobox)
    public static final String DUALIS_PERSON_TYPE = "dualisPersonType";
    // editierbare Combobox, wenn in veranstaltungstyp, coursetyp, personentyp attr fehlt, dann Fragen und anlegen (nur Textattribute)
    public static final String DUALIS_PK_ATTRIBUTE = "dualisPK";
    // event typ -> fragen und anlegen, wenn nicht da
    public static final String DUALIS_SOLL_ATTRIBUTE = "dualisSoll";
    // combobox auswaehlen
    public static final String DUALIS_NAME_ATTRIBUTE = "name";
    
    public static final int FACULTY_COLUMN = 1;
    
    public static final int PROGRAM_OF_STUDY_COLUMN = 1;
    
    public static final int SEMESTER_COLUMN =    1;

 	public static final int COURSE_COLUMN = 1 ;
    public static final int PERSON_PK_COLUMN = 0;
    public static final int EVENT_TITLE_COLUMN = 3;
    public static final int COURSE_PK_COLUMN = 4;
    public static final int SOLL_COLUMN = 8;
    public static final String DUALIS_ROOM_TYPE = "dualisRoomType";

    /**
     * This method returns how the name of the Plug-in should be shown in the
     * administrator options.
     */
    public String toString() {
        return "Dualis Import Plugin";
    }

    public void provideServices(ClientServiceContainer container, Configuration config) {
        // check if the plug-in is already available
        if (!config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT))
            return;
        // this service allows to use own language information provided by the
        // ExtendedTableViewResources.xml
        container.addContainerProvidedComponent(RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(RESOURCE_FILE.getId()));
        container.addContainerProvidedComponent(RaplaClientExtensionPoints.RESERVATION_WIZARD_EXTENSION, DualisImportWizard.class);
        container.addContainerProvidedComponent(RaplaClientExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, DualisImportAdminOption.class);

    }



}