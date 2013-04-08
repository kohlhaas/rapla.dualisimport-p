package org.rapla.plugin.dualisimport;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.rapla.components.util.Tools;

public class CSVImport {
    private String[][] entries;
    private ArrayList<String> faculties;
    private ArrayList<String> programOfStudy;
    private ArrayList<String> semester;
    private ArrayList<String> course;

    public CSVImport(Reader reader) throws IOException {
        entries = Tools.csvRead(reader, 15);
        faculties = new ArrayList<String>();
        programOfStudy = new ArrayList<String>();
        semester = new ArrayList<String>();
        course = new ArrayList<String>();
        for (int i = 1; i < entries.length; i++) {
            if (entries[i][8] != null) {
                if (!faculties.contains((entries[i][8]))) {
                    faculties.add(entries[i][8]);
                }
            }
            if (entries[i][9] != null) {
                if (!programOfStudy.contains((entries[i][9]))) {
                    programOfStudy.add(entries[i][9]);
                }
            }
            if (entries[i][11] != null) {
                if (!semester.contains((entries[i][11]))) {
                    semester.add(entries[i][11]);
                }
            }
            if (entries[i][7] != null) {
                if (!course.contains((entries[i][7]))) {
                    course.add(entries[i][7]);
                }
            }
        }
    }

    public void extractUserAttributes(String[][] table) {
        String[][] export = new String[table.length][5];
        for (int i = 0; i < table.length; i++) {
            export[i][0] = table[i][3].substring(0, 0) + "." + table[i][4];
            export[i][1] = "";
            export[i][2] = table[i][3] + " " + table[i][4];
            export[i][3] = "";
            export[i][4] = "";
        }
        String fileName = "c:\\temp\\users.csv"; //location of generated report

        try {
            FileWriter writer = new FileWriter(fileName);
            for (int i = 0; i < export.length; i++) {
                for (int j = 0; j < export[i].length; j++) {
                    writer.append(export[i][j]);
                    if (j < export[j].length - 1)
                        writer.append(',');
                    else
                        writer.append('\n');
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String[][] getEntries() {
        return entries;
    }

    public ArrayList<String> getFaculties() {
        return faculties;
    }

    public ArrayList<String> getProgramOfStudy() {
        return programOfStudy;
    }

    public ArrayList<String> getSemester() {
        return semester;
    }

    public ArrayList<String> getCourse() {
        return course;
    }

    public String[][] trimToColumn(String[][] rows, String keyWord, int column) {
        int counter = 0;
        for (String[] row : rows) {
            if (row[column] != null) {
                if (row[column].contains(keyWord))
                    counter = counter + 1;
            }
        }
        String[][] columnTrim = new String[counter][rows[1].length];
        int k = 0;
        for (String[] row : rows) {
            if (row[column] != null) {
                if (row[column].equals(keyWord)) {
                    System.arraycopy(row, 0, columnTrim[k], 0, row.length);
                    k = k + 1;
                }
            }
        }
        return columnTrim;
    }

}

