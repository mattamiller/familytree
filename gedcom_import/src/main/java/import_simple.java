import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.graph.GraphOptions;
import com.datastax.driver.dse.graph.GraphProtocol;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.gedcom4j.exception.GedcomParserException;
import org.gedcom4j.model.*;
import org.gedcom4j.parser.GedcomParser;
import java.util.Date;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class import_simple <date> {
    public static void main(String[] args) {
        DseSession session = ConnectToDSE();
        GedcomParser gp = new GedcomParser();

        //Get all the files in the samples directory and iterate over them
        File dir = new File("./samples/original_samples/");
        File[] files = dir.listFiles();
        for (File file: files) {
            if (!file.getName().startsWith(".")) {
                System.out.println("Beginning Import of " + file.getName());
                try {
                    gp.load(file.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GedcomParserException e) {
                    e.printStackTrace();
                }

                //This gets the actual GEDCOM document
                Gedcom g = gp.getGedcom();
                UUID treeId = UUID.randomUUID();

                int cnt = 0;
                for (Individual i : g.getIndividuals().values()) {
                    insertIndividual(session, treeId, i);
                    cnt++;
                    if (cnt > 0 && cnt % 100 == 0) {
                        System.out.println("Writing out person " + cnt);
                    }
                }

                cnt = 0;
                for (Family f : g.getFamilies().values()) {
                    insertFamily(session, treeId, f);
                    cnt++;
                    if (cnt > 0 && cnt % 100 == 0) {
                        System.out.println("Writing out family " + cnt);
                    }
                }

                insertTreeNode(session, treeId, Iterables.get(g.getIndividuals().values(), 0).getXref());
            }
        }
        System.out.println("Import Complete");
        System.exit(1);
    }
    public static DseSession ConnectToDSE() {
        DseCluster cluster;
        try {
            cluster = DseCluster.builder()                                               // (1)
                    .addContactPoint("localhost")
                    .withGraphOptions(new GraphOptions()
                            .setGraphName("fs_gene_02")
                            .setGraphSubProtocol(GraphProtocol.GRAPHSON_3_0))
                    .build();
            DseSession session = cluster.connect();                                      // (2)
            return session;
        } catch (Exception ex)
        {
            System.out.println("DSE Exception: " + ex.toString());
            return null;
        }
    }

    public static void insertIndividual(DseSession session, UUID treeId, Individual person) {

        UUID id = UUID.randomUUID();


        try {
            String full_name = person.getFormattedName();
            System.out.println(full_name);
            String first_name;
            String last_name;

            first_name = full_name.substring( 0, full_name.indexOf(" /"));
            last_name = full_name.substring(full_name.indexOf(" /"), full_name.indexOf("/,"));

            session.executeGraph("g.addV('person')"
                        +".property('tree_id', tree_id)"
                        +".property('last_name', last_name)"
                        +".property('unique_id', unique_id)"
                        +".property('full_name', full_name)"
                        +".property('first_name', first_name)"
                        +".property('sex', sex)"
                        +".property('GEDCOM_xref_id', gedcom_id)"
                        +".property('birthdate', birthdate)"
                ,
                ImmutableMap.<String, Object>builder()
                        .put("tree_id", treeId)
                        .put("last_name", last_name)
                        .put("unique_id", id)
                        .put("full_name", person.getFormattedName() != null ?
                                person.getFormattedName().toString() : "")
                        .put("first_name", first_name)
                        .put("sex", person.getSex() != null ?
                                person.getSex().toString() : "")
                        .put("gedcom_id", person.getXref().toString())
//                          .put("birthdate", person.getEventsOfType(IndividualEventType.BIRTH))
                        .put("birthdate", new Date().toString())

                        .build());
        } catch (Exception ex) {
            System.out.println("Person: " + ex.toString() + person.toString());
        }
//        for (PersonalName n : person.getNames()) {
//
//            try {
//                session.executeGraph("g.V().has('person', 'unique_id', person_id)" +
//                                ".as('i').addE('is_known_as').from('i').to(" +
//                                "__.addV('name')" +
//                                ".property('full_name', full_name)" +
//                                ".property('given_name', given_name)" +
//                                ".property('last_name', last_name)" +
//                                ".property('nick_name', nick_name)" +
//                                ".property('prefix', prefix)" +
//                                ".property('last_name_prefix', last_name_prefix)" +
//                                ".property('suffix', suffix))",
//                        ImmutableMap.<String, Object>builder()
//                                .put("person_id", id)
//                                .put("full_name", n.getBasic() != null ?
//                                        n.getBasic() : "")
//                                .put("given_name", n.getGivenName() != null ?
//                                        n.getGivenName().getValue() : "")
//                                .put("last_name", n.getSurname() != null ?
//                                        n.getSurname().getValue() : "")
//                                .put("nick_name", n.getNickname() != null ?
//                                        n.getNickname().getValue() : "")
//                                .put("prefix", n.getPrefix() != null ?
//                                        n.getPrefix().getValue() : "")
//                                .put("last_name_prefix", n.getSurnamePrefix() != null ?
//                                        n.getSurnamePrefix().getValue() : "")
//                                .put("suffix", n.getSuffix() != null ?
//                                        n.getSuffix().getValue() : "")
//                                .build());
//            } catch (Exception ex) {
//                System.out.println("Individual Name: " + ex.toString() + n.toString());
//            }
//        }

    }

    public static void insertFamily(DseSession session, UUID treeId, Family family) {

        UUID id = UUID.randomUUID();
        try {
            session.executeGraph("g.addV('family')"
                            +".property('unique_id', family_id)"
                            +".property('GEDCOM_xref_id', GEDCOM_xref_id)"
                            +".property('husband_id', husband_unique_id)"
                            +".property('tree_id', tree_id)"
                            +".property('count_of_children', count_of_children)"
                            +".property('wife_id', wife_unique_id).as('f')"
                            +".V().has('person', 'GEDCOM_xref_id', husband_unique_id)"
                            +".has('tree_id', tree_id)"
                            +".addE('member_of').to('f').property('relationship', 'Husband')"
                            +".V().has('person', 'GEDCOM_xref_id', wife_unique_id)"
                            +".has('tree_id', tree_id)"
                            +".addE('member_of').to('f').property('relationship', 'Wife')"
                    ,
                    ImmutableMap.<String, Object>builder()
                            .put("family_id", id)
                            .put("GEDCOM_xref_id", family.getXref())
                            .put("count_of_children", family.getNumChildren() != null ? family.getNumChildren() : 0)
                            .put("wife_unique_id", family.getWife() != null ?
                                    family.getWife().getIndividual().getXref() : "")
                            .put("husband_unique_id", family.getHusband() != null ?
                                    family.getHusband().getIndividual().getXref() : "")
                            .put("tree_id", treeId)
                            .build());
        } catch (Exception ex) {
            System.out.println("Family: " + ex.toString() + family.toString());
        }

        if (family.getChildren()!=null) {
            for (IndividualReference c : family.getChildren()) {
                if (c.getIndividual()!=null) {
                    try {
                        session.executeGraph("g.V()" +
                                        ".has('family', 'unique_id', family_id)" +
                                        ".has('tree_id', tree_id).as('f')" +
                                        ".V().has('person', 'tree_id', tree_id)" +
                                        ".has('GEDCOM_xref_id', child_xref_id)" +
                                        ".addE('member_of').to('f')" +
                                        ".property('relationship', relationship)",
                                ImmutableMap.<String, Object>builder()
                                        .put("family_id", id)
                                        .put("tree_id", treeId)
                                        .put("child_xref_id", c.getIndividual().getXref())
                                        .put("relationship", c.getIndividual().getSex()
                                                .getValue().toUpperCase().startsWith("M")
                                                ? "Son" : "Daughter")

                                        .build());
                    } catch (Exception ex) {
                        System.out.println("Family Children: " + ex.toString() + c.toString());
                    }
                }
            }
        }

    }

    private static void insertTreeNode(DseSession session, UUID treeId, String ownerXrefId) {
        try {
            session.executeGraph("g.addV('tree')" +
                            ".property('unique_id', tree_id)" +
                            ".as('t')" +
                            ".coalesce(__.V().has('person', 'tree_id', tree_id)" +
                            ".has('person', 'GEDCOM_xref_id', owner_id)," +
                            "__.addV('person').property('tree_id', tree_id)" +
                            ".property('GEDCOM_xref_id', owner_id)).addE('owns').to('t')"
                    ,
                    ImmutableMap.<String, Object>builder()
                            .put("tree_id", treeId)
                            .put("owner_id", ownerXrefId)
                            .build());
        } catch (Exception ex) {
            System.out.println("Tree Node: " + ex.toString() + treeId);
        }
    }
}