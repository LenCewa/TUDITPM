# IT Praktikum

In dem Repository werden das IT Praktikum gespeichert.

## Readme.md

In der Readme.md sollen die grundlegenden Nutzungsmöglichkeiten für das Git Repository erklärt werden.

## Installation Kafka

kafka installation (+zookeeper):
https://dzone.com/articles/running-apache-kafka-on-windows-os

eclipse plugins:
https://cwiki.apache.org/confluence/display/KAFKA/Developer+Setup

gradle für windows(binary only distribution klicken):
https://gradle.org/gradle-download/ 

vorausgesetzt jeder hat git installiert

## Installation MongoDB
Community Server (Download msi)
https://www.mongodb.com/download-center?jmp=nav#community

Startbefehl:

1. Konsole im MongoDB .\bin Ordner öffnen

2. Befehl: mongod --dbpath C:\\Users\Ludwig\Documents\MongoDB\data

3. Der Teil nach --dbpath gibt an wo ihr eure Daten gespeichert haben wollt

## Branchstruktur

Die folgenden Branches sollen genutzt werden:
  - develop 
  - kafka
    - kafka/master
    - kafka/feature
      - kafka/feature/anbindung - z.B. erste Datenanbindung
  - spark
    - spark/master
    - spark/feauture
      - spark/feature/analytics - z.B. textAnalytics


