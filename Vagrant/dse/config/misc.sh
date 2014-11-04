#!/usr/bin/env bash

echo ">>> Installing NTP"
sudo apt-get install ntp
echo ">>> Finished Installing NTP"

echo ">>> Installing handy stuff"
sudo apt-get sysstat install avahi-utils unzip vim curl zip python-software-properties python-pip git -y

echo ">>> Installing GoLang and pcstat"
sudo apt-get install golang -y

sudo echo 'export GOPATH=/usr/share/go/' >> /etc/profile
sudo echo 'export PATH=$PATH:$GOPATH/bin' >> /etc/profile
sudo go get github.com/tobert/pcstat

echo ">>> Adding OS tunings"
sudo swapoff --all
sudo echo "vm.max_map_count = 131072" >> /etc/sysctl.conf
sudo sysctl -p