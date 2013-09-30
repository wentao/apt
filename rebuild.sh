#!/bin/bash

cp src/main/java/com/gm/model/dao.ftl target/classes/com/gm/model/ ; mvn compile ; mvn package ; mvn install
