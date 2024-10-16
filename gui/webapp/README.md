# RDFRules: GUI operations documentation

## Dataset

### Loading

#### Load graph

Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF or relational SQL format and is supposed as a single graph.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name or upload a new one), or you can load any remote file from URL (see below). The dataset format is detected automatically by the file extension. Supported extensions are .ttl (turtle), .nt (n-triples), .nq (n-quads), .json | .jsonld (JSON-LD), .xml | .rdf | .owl (RDF/XML), .trig (TriG), .trix (TriX), .tsv, .sql, .cache (internal binary format). All formats can be compressed by GZIP or BZ2 (e.g. data.ttl.gz).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **Graph name**: Name for this loaded graph. It must have the URI notation in angle brackets, e.g., <dbpedia> or `<http://dbpedia.org>`.

#### Load dataset

Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF or relational SQL format and can involve several graphs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name or upload a new one), or you can load any remote file from URL (see below). The dataset format is detected automatically by the file extension. Supported extensions are .ttl (turtle), .nt (n-triples), .nq (n-quads), .json | .jsonld (JSON-LD), .xml | .rdf | .owl (RDF/XML), .trig (TriG), .trix (TriX), .tsv, .sql, .cache (internal binary format). All formats can be compressed by GZIP or BZ2 (e.g. data.ttl.gz).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.

### Transformations

#### Add prefixes

Add prefixes to datasets to shorten URIs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file with prefixes in the Turtle (.ttl) format from the workspace on the server side (just click onto a file name), or you can load any remote prefix file from URL (see below).
- **URL**: A URL to a remote file with prefixes in the Turtle (.ttl) format to be loaded. If this is specified then the workspace file is ommited.
- **Hand-defined prefixes**: Here, you can define your own prefixes manually.
  - **Prefix**: A short name for the namespace.
  - **Namespace**: A namespace URI to be shortened. It should end with the slash or hash symbol, e.g., `http://dbpedia.org/property/`.

#### Merge datasets

Merge all previously loaded graphs and datasets to one dataset.

#### Map quads

Map/Replace selected quads and their parts by user-defined filters and replacements.

##### Properties

- **Search**: Search quads by this filter and then replace found quads by defined replacements. Some filters can capture parts of quads and their contents.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Graph**: Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`. The content is evaluated as a regular expression.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).
- **Replacement**: Replace found quads and their parts with replacements. Here, you can also refer to captured parts in regular expressions.
  - **Subject**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $p1 = captured group in regexp in the predicate position.
  - **Predicate**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.
  - **Object**: For RESOURCE, the replacement can be only the full URI or blank node (prefixed URI is not allowed). For TEXT, the replacement must start and end with double quotes. For NUMBER, the replacement must be a number or some arithmetic evaluation with captured value, e.g., $o0 + 5 (it adds 5 to original numeric value). For BOOLEAN, there are only two valid values true|false. For INTERVAL, the replacement has the interval form, e.g, (10;80] (both borders of the found interval are captured, we can refer to them in replacement: [$o1;$o2]). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.
  - **Graph**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.

#### Shrink

Shrink the dataset (set of quads) with a specified window.

##### Properties

- **Strategy**: Take (take first n records), Drop (drop first n records), Slice (slice a window)

#### Discretize

Discretize all numeric literals related to filtered quads by a selected discretization strategy.

##### Properties

- **Subject**: Discretize all numeric literals which are related to this specifed subject. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule.
- **Predicate**: Discretize all numeric literals which are related to this specifed predicate. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label.
- **Object**: Discretize all numeric literals which are matching this object. If this field is empty then no filter is applied here. The object must be a numeric comparison, e.g, '> 10' or '(10;80]'.
- **Graph**: Discretize all numeric literals which are related to this specifed graph. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`.
- **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).
- **Strategy**: Equidistance - all numbers are grouped into n bins, where each bin has same interval length. Equifrequency - the algorithm tries to make n intervals where all intervals are as similar in size (cardinality) to each other as possible. Equisize - it is similar to the Equifrequency strategy, but the main threshold is the max relative interval support (size, cardinality) which each interval should reach.
  - **Equidistance or Equifrequency**:
    - **Number of bins**: Number of intervals to be created.
  - **Equisize**:
    - **Min support**: The minimal relative support which must reach each interval. The valid range is between 0 and 1. 

#### Cache

Cache loaded dataset into memory or a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

#### Filter quads

Filter all quads by user-defined conditions.

##### Properties

- **Filter by (logical OR)**: Defined quad filters. It filters such quads which satisfy defined conditions.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Graph**: Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`. The content is evaluated as a regular expression.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).

#### Load graph

Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and is supposed as a single graph.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.
- **Graph name**: Name for this loaded graph. It must have the URI notation in angle brackets, e.g., <dbpedia> or `<http://dbpedia.org>`.

#### Load dataset

Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and can involve several graphs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.

#### Index

Build an in-memory fact index from the loaded dataset. This operation may consume lots of memory depending on the dataset size.

### Actions

#### Export

Export the loaded and transformed dataset into a file in the workspace in an RDF format.

##### Properties

- **Path**: Choose a file in the workspace (or create a new one) where the exported dataset should be saved. The dataset format is detected automatically by the file extension. Supported extensions are .ttl (turtle), .nt (n-triples), .nq (n-quads), .trig (TriG), .trix (TriX), .tsv, .cache (internal binary format). All formats can be compressed by GZIP or BZ2 (e.g. data.ttl.gz).

#### Get quads

Get and show first 10000 quads from the loaded dataset.

#### Get prefixes

Show all prefixes defined in the loaded dataset.

#### Size

Get number of quads from the loaded dataset.

#### Properties

Get all properties and their ranges with sizes.

#### Histogram

Aggregate triples by their parts and show the histogram.

##### Properties

- **Subject**: Aggregate quads by subjects.
- **Predicate**: Aggregate quads by predicates.
- **Object**: Aggregate quads by objects.

## Index

### Loading

#### Load index

Load a serialized fact index from a file in the workspace.

##### Properties

- **Choose a file from the workspace**: You can load a serialized index file from the workspace on the server side (just click onto a file name).
- **Partial loading**: If the index is used only for mapping of triple items then the fact indices of triples are not loaded.

### Transformations

#### To dataset

Convert the memory index back to the dataset.

#### Mine rules

Mine rules from the indexed dataset with user-defined threshold, patterns and constraints.

##### Properties

- **Thresholds**: Mining thresholds. For one mining task you can specify several thresholds. All mined rules must reach defined thresholds. This greatly affects the mining time. 
  - **MinHeadCoverage**:
    - **Value**: The minimal value is 0.001 and maximal value is 1.
  - **MinHeadSize or MinSupport or Timeout**:
    - **Value**: The minimal value is 1.
  - **MaxRuleLength**:
    - **Value**: The minimal value is 2.
  - **MinAtomSize**:
    - **Value**: If negative value, the minimal atom size is same as the current minimal support threshold.
- **Rule consumer**: Settings of the rule consumer to which all mined closed rules are saved.
  - **Top-k**: This activates the top-k approach, where top k rules with the highest support are mined. This approach can rapidly speed up the mining time.
    - **k-value**: A k value for the top-k approach. The minimal value is 1.
    - **Allow overflow**: If there are multiple rules with the lowest head coverage in the priority queue, then all of them may not be saved into the queue since the k value can not be exceeded. For this case the ordering of such rules is not deterministic and same task can return different results due to the long tail of rules with a same head coverage. If you check this, the overflowed long tail will be also returned but you can get much more rules on the output than the k value.
  - **On disk**: If this is checked all mined rules are gradually saving on disk instead of memory.
    - **Export path**: A relative path to a file where the rules will be continuously saved in a pretty printed format.
    - **Export rules format**: 
- **Patterns**: In this property, you can define several rule patterns. During the mining phase, each rule must match at least one of the defined patterns. Rule pattern has the following grammar: (? ? ?) ^ (? ? ?) => (? ? ?), ? is any atom item, ?V is any variable, ?C is any constant, ?a is the particular variable. You can also specify any constant instead of a variable. Symbol * matches any remaining body atoms, e.g, * ^ (? ? ?) => (? ? ?).
- **Constraints**: Within constraints you can specify whether to mine rules without constants or you can include only chosen predicates into the mining phase.
  - **OnlyPredicates or WithoutPredicates**:
    - **Values**: List of predicates. You can use prefixed URI or full URI in angle brackets.
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

#### Load ruleset

Load serialized ruleset from a file in the workspace.

##### Properties

- **Choose a file from the workspace**: You can load a serialized ruleset file from the workspace on the server side (just click onto a file name).
- **Rules format**: The ruleset format. Default is "NDJSON".
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

#### Load prediction

Load serialized predicted triples from a file in the workspace.

##### Properties

- **Choose a file from the workspace**: You can load a serialized prediction file from the workspace on the server side (just click onto a file name).
- **Prediction format**: The prediction format. Default is "NDJSON".
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

#### Cache

Cache loaded index into memory or a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

### Actions

#### Export

Serialize and export loaded index into a file in the workspace at the server side for later use.

##### Properties

- **Path**: A relative path to a file related to the workspace where the serialized index should be saved.

#### Properties cardinality

Get properties cardinalities (size, domain, range)

##### Properties

- **Filter**: A list of properties to be analyzed. The empty list means all properties.

## Ruleset

### Loading

#### Load ruleset

Load serialized ruleset from a file in the workspace without a fact index. No operations requiring a fact index are not permitted. 

##### Properties

- **Choose a file from the workspace**: You can load a serialized ruleset file from the workspace on the server side (just click onto a file name).
- **Rules format**: The ruleset format. Default is "NDJSON".
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

### Transformations

#### Filter

Filter all rules by patterns or measure conditions.

##### Properties

- **Patterns**: In this property, you can define several rule patterns. During the filtering phase, each rule must match at least one of the defined patterns.
- **Measures**: Rules filtering by their interest measures values.
  - **Name**:
  - **Value**: Some condition for numerical comparison, e.g, '> 0.5' or '(0.8;0.9]' or '1.0'

#### Shrink

Shrink the ruleset (set of rules) with a specified window.

##### Properties

- **Strategy**: Take (take first n records), Drop (drop first n records), Slice (slice a window)

#### Sort

Sort rules by user-defined rules attributes.

#### Cache

Cache loaded ruleset into memory or a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

#### Predict

Use rules in the ruleset to predict triples depending on the loaded fact index.

##### Properties

- **Predicted triple constraints**: You can specify which predicted triples should be returned. Positive means all correctly predicted triples which are contained in the KG. Negative means all incorrectly predicted triples which are not contained in the KG. PCA positive means all predicted triples which are not contained in the KG, but within partial-completeness assumption, they can not be regarded as incorrect prediction.
- **Injective mapping**: Same constant can not be bound with two different variables.

#### Prune

Perform a pruning strategy with all rules in the ruleset

##### Properties

- **Strategy**: Data coverage pruning - from the list of rules we gradually take such rules which cover all generated triples from the input dataset. Closed - we take only rules within closure, it means all sub-rules of a selected rule are less frequent than the selected rule. Maximal - rules which do not have any frequent sub-rules. SkylinePruning - all rules which have lower selected measure than its parents are pruned.
- **Data coverage pruning**:
  - **Only functional properties**: Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).
  - **Only existing triples**: If checked, the common CBA strategy will be used. That means we take only such predicted triples, which are contained in the input dataset. This strategy takes maximally as much memory as the number of triples in the input dataset. If false, we take all predicted triples (including triples which are not contained in the input dataset and are newly generated). For deduplication a HashSet is used and therefore the memory may increase unexpectedly because we need to save all unique generated triples into memory..
- **Closed or SkylinePruning**:
  - **Measure**: Some measure by which to do this pruning strategy.

#### Compute confidence

Compute the confidence for all rules and filter them by a minimal threshold value.

##### Properties

- **Name**: CWA (standard confidence with closed-world assumption), PCA confidence (confidence with partial-completeness assumption) or Lift
- **CWA confidence**:
  - **Min confidence**: A minimal confidence threshold. This operation counts the standard confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **PCA confidence**:
  - **Min PCA confidence**: A minimal PCA (Partial Completeness Assumption) confidence threshold. This operation counts the PCA confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **Lift**:
  - **Min confidence**: A minimal confidence threshold. This operation first counts the standard confidence for all rules and filter them by this minimal threshold, then it counts the lift measure by the computed cofindence. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **Top-k**: Get top-k rules with highest confidence. This is the optional parameter.

#### Make clusters

Make clusters from the ruleset by DBScan algorithm.

##### Properties

- **Min neighbours**: Min number of neighbours to form a cluster.
- **Min similarity**: Min similarity between two rules to form a cluster.

#### To graph-aware rules

Attach information about graphs belonging to output rules.

### Actions

#### Instantiate

Instantiate a rule

#### Export

Export the ruleset into a file in the workspace.

##### Properties

- **Path**: A relative path to a file related to the workspace where the exported rules should be saved.
- **Rules format**: The 'text' format is human readable, but it can not be parsed for other use in RDFRules, e.g. for completion another dataset. If you need to use the ruleset for other purposes, use the 'ndjson', 'json' format or the 'cache' process.

#### Get rules

Get and show first 10000 rules from the ruleset.

#### Size

Get number of rules from the ruleset.

## Prediction

### Loading

#### Load prediction

Load serialized predicted triples from a file in the workspace without a fact index. No operations requiring a fact index are not permitted.

##### Properties

- **Choose a file from the workspace**: You can load a serialized prediction file from the workspace on the server side (just click onto a file name).
- **Prediction format**: The prediction format. Default is "NDJSON".
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

### Transformations

#### Shrink

Shrink the prediction (set of predicted triples) with a specified window.

##### Properties

- **Strategy**: Take (take first n records), Drop (drop first n records), Slice (slice a window)

#### Filter

Filter all predicted triples by selected conditions.

##### Properties

- **Predicted triple constraints**: You can specify which predicted triples should be returned. Positive means all correctly predicted triples which are contained in the KG. Negative means all incorrectly predicted triples which are not contained in the KG. PCA positive means all predicted triples which are not contained in the KG, but within partial-completeness assumption, they can not be regarded as incorrect prediction.
- **Distinct predictions**: Each predicted triple can be determined only by one rule. Other rules are skipped.
- **Functions only**: All predictions are function. It means we can predict only one object for a particular subject and predicate. Other predictions are skipped.
- **Triple filter**: Defined triple filters. It filters such predicted triples which satisfy defined conditions.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).
- **Patterns**: In this property, you can define several rule patterns. During the filtering phase, each rule must match at least one of the defined patterns.
- **Measures**: Rules filtering by their interest measures values.
  - **Name**:
  - **Value**: Some condition for numerical comparison, e.g, '> 0.5' or '(0.8;0.9]' or '1.0'

#### Sort

Sort predicted triples by their rules.

#### To dataset

Transform all predicted triples to the dataset structure.

#### Cache

Cache predicted triples into memory or a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized prediction should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

### Actions

#### Export

Export predicted triples into a file in the workspace.

##### Properties

- **Path**: A relative path to a file related to the workspace where the exported predicted triples should be saved.

#### Get predicted triples

Get and show first 10000 predicted triples.

##### Properties

- **Group by triples**: Duplicit predicted triples are grouped with a list of all rules predicting such rule.

#### Size

Get number of all predicted triples.

## PredictionTasks

### Transformations

#### Shrink

Shrink the prediction tasks with a specified window.

##### Properties

- **Strategy**: Take (take first n records), Drop (drop first n records), Slice (slice a window)

### Actions

#### Evaluate

Evaluate all prediction tasks. It counts all entities (which should be covered by predictions), TP, FP, FN, Hits@K, MRR and evaluate precision, recall, etc.

##### Properties

- **Evaluation target**: Test set - entities are generated from test set triples covered by each prediction task. Prediction - entities are generated by all predicted triples for each prediction task

#### Size

Get number of all prediction tasks.