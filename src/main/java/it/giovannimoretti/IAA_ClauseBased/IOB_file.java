package it.giovannimoretti.IAA_ClauseBased;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IOB_file {
    private File file;
    private String classe;
    private List<List<String>> clauses;
    private List<Integer> classes;
    private boolean annotated = false;
    private String filename;


    public IOB_file(File file, String clas) {
        this.file = file;
        this.classe = clas;
        this.filename = file.getName();
       // System.out.println("F" +  file);
        List<List<String>> clauses = new ArrayList<>();
        List<Integer> classes = new ArrayList<>();

        try {
            List<String> lines = Files.readLines(this.file,
                    Charsets.UTF_8);
            lines.add("\n");

            List<String> clause = new ArrayList<>();
            for (String line : lines) {
                if (line.trim().length() > 0) {
                    clause.add(line);
                } else {
                    if (clause.size() > 0) {
                        clauses.add(clause);
                        clause = new ArrayList<>();
                    }
                }
            }

            for (List<String> c : clauses){
                String val;
                try {
                    val = c.get(0).split("\t")[1].split("-")[1];
                }catch (Exception e){
                   // System.err.println("Abnormal value for clause " + c.toString() + " value UNSET");
                    val = "UNSET";
                }

                if (val.equals(this.classe)){
                    classes.add(1);
                }else{
                    classes.add(0);
                }
                for (int i = 0; i < c.size(); i++) {
                    c.set(i,c.get(i).split("\t")[0]);
                }
            }
            this.clauses = clauses;
            this.classes= classes;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public boolean isAnnotated() {
        return annotated;
    }

    public List<List<String>> getClauses() {
        return clauses;
    }

    public List<Integer> getClasses() {
        return classes;
    }

    public String getFilename() {
        return filename;
    }
}
