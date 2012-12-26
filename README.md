Batch-Importer NEO4j批量数据导入工具
--------------------------------------

Batch-Importer 是基于[batch-import](https://github.com/jexp/batch-import)修改的`Neo4j (CSV) Batch Importer`, 原来的[batch-import](https://github.com/jexp/batch-import)使用起来有些不方便，局限性在：__批量导入时需要同时指定node.csv 和 relationship.csv__. 实际批量导入时往往会遇到如下需求：

* 只批量导入节点，节点有不同的属性。
* 只批量导入关系，关系有不同的type。
* 只批量导入索引。

所以做了一点赶紧，支持如下三种用法：

###批量导入不同属性的节点###
```
java -jar batch-import.jar neo4j-db-file node node1.csv [node2.csv, ...]
```

* 例如node1.csv的格式可以如下：


 name | update_time | status | type | label |
 :------------:|:------------:|:------------:|:------------:|:------------:|
 a | 2012-12-26 16:43:52.644741 | new | monitor | {name};{type} |
 b | 2012-12-26 16:43:52.645970 | new | monitor | {name};{type} |


 * 例如node2.csv的格式可以如下：

  server_id | server_name | server_status | update_time | status | type | label |
  :------------:|:------------:|:------------:|:------------:|:------------:|:------------:|
  154335 | a | Available | 2012-12-26 16:43:52.644741 | new | 服务器 | {name};{type} |
  125977 | b | Available | 2012-12-26 15:42:04.925568 | new | 服务器 | {name};{type} |
   

###批量建立节点的索引###
```
java -jar batch-import.jar neo4j-db-file nodeindex node_index1.csv [node_index2.csv, ...]
```

* 例如nodeindex1.csv格式如下：

 id | name |
 :------------:|:------------:|
 121742 | 机器名1 |
 135225 | 机器名2 |


 * 例如nodeindex2.csv格式如下：

  id | task_id |
  :------------:|:------------:|
  121742 | 任务id1 |
  135225 | 任务id2 |

###批量建立关系###
```
java -jar batch-import.jar neo4j-db-file rel relationship1.csv [relationship1.csv, ...]
```

* 例如relationship1.csv的格式如下：

 start | end | type | property | 
 :------------:|:------------:|:------------:|:------------:|
 125653 | 261855 | 输出 | 输出 |
 36620 | 261900 | 输出 | 输出 |
 31858 | 261867 | 属于 | 属于 |
 4358 | 2373545 | 发布 | 发布 |


