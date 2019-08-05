//create properties first
//Shared
schema.propertyKey("unique_id").Uuid().ifNotExists().create()
schema.propertyKey("GEDCOM_xref_id").Text().ifNotExists().create()
schema.propertyKey("tree_id").Text().ifNotExists().create()
//Family
schema.propertyKey("husband_id").Text().ifNotExists().create()
schema.propertyKey("wife_id").Text().ifNotExists().create()
schema.propertyKey("count_of_children").Text().ifNotExists().create()
//Individual
schema.propertyKey('restriction').Text().ifNotExists().create()
schema.propertyKey('sex').Text().ifNotExists().create()
//Member_of Edge
schema.propertyKey("relationship").Text().ifNotExists().create()
//Individual And Name
schema.propertyKey("full_name").Text().ifNotExists().create()
//Name
schema.propertyKey("given_name").Text().ifNotExists().create()
schema.propertyKey("last_name").Text().ifNotExists().create()
schema.propertyKey("nick_name").Text().ifNotExists().create()
schema.propertyKey("prefix").Text().ifNotExists().create()
schema.propertyKey("last_name_prefix").Text().ifNotExists().create()
schema.propertyKey("suffix").Text().ifNotExists().create()

//create vertices
schema.vertexLabel("family").partitionKey('tree_id').clusteringKey('unique_id').properties('unique_id', 'tree_id', 'husband_id', 'wife_id', 'GEDCOM_xref_id', 'count_of_children').ifNotExists().create()
schema.vertexLabel("individual").partitionKey('tree_id').clusteringKey('unique_id').properties('unique_id', 'tree_id', 'full_name', 'sex', 'GEDCOM_xref_id').ifNotExists().create()
schema.vertexLabel("name").properties('full_name', 'given_name', 'last_name', 'nick_name', 'prefix', 'last_name_prefix', 'suffix').ifNotExists().create()
schema.vertexLabel("tree").properties('unique_id').ifNotExists().create()

//create edges
schema.edgeLabel("owns").connection("individual", "tree").ifNotExists().create()
schema.edgeLabel("member_of").properties('relationship').connection("individual", "family").ifNotExists().create()
schema.edgeLabel("is_known_as").connection("individual", "name").ifNotExists().create()

//add Indexes
schema.vertexLabel('individual').index('byGivenName').materialized().by('given_name').ifNotExists().add()
schema.vertexLabel('individual').index('relationship').outE('member_of').by('relationship').ifNotExists().add()


// FAMILY SEARCH
//// Verticies
//schema.vertexLabel("person")
//        .ifNotExists()
//        .partitionBy("person_id",UUID)
//        .property("personId",Text) // (Ex: RichardLewis406)
//        .property("first_names",Text) // (Ex: Richard Charles)
//        .property("last_names",Text) // (Ex: Lewis, Doane-Lewis)
//        .property("sex", Text)
//        .property("birthdate", Text)
//        .property("christening", Text)
//        .property("death", Text)
//        .property("burial", Text)
//        .create()
//
//// Edges
//schema.edgeLabel("knows")
//        .ifNotExists()
//        .from("person")
//        .to("person")
//        .property("relation", Text)
//        .create()