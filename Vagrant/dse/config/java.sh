#!/usr/bin/env bash

#Install Oracle Java 7 without prompt
echo ">>> Installing Oracle Java 7"
echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install oracle-java7-installer -y -q
sudo update-java-alternatives -s java-7-oracle
sudo apt-get install oracle-java7-set-default -y
echo ">>> Finished installing Oracle Java 7"

echo ">>> Installing JNA"
sudo apt-get install libjna-java -y
echo ">>> Finished Installing JNA"