#!/usr/bin/env bash

echo ">>> Installing NTP"
sudo apt-get install ntp
echo ">>> Finished Installing NTP"


echo ">>> Adding OS tunings"
sudo swapoff --all
sudo echo "vm.max_map_count = 131072" >> /etc/sysctl.conf
sudo sysctl -p
