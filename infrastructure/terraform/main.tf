terraform {
  required_providers {
    multipass = {
      source  = "larstobi/multipass"
      version = "~> 1.4.2"
    }
  }
}

provider "multipass" {}

# Creazione del nodo Master per il cluster K3s
resource "multipass_instance" "master" {
  name   = "k3s-master"
  cpus   = 2
  memory = "2G"
  disk   = "10G"
  image  = "jammy" # Ubuntu 22.04 LTS
}

# Creazione del nodo Worker per il cluster K3s
resource "multipass_instance" "worker" {
  name   = "k3s-worker"
  cpus   = 2
  memory = "2G"
  disk   = "10G"
  image  = "jammy"
}

# Genera l'inventory per Ansible automaticamente estraendo gli IP
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/../ansible/inventory.tmpl", {
    master_ip = multipass_instance.master.ipv4
    worker_ip = multipass_instance.worker.ipv4
  })
  filename = "${path.module}/../ansible/inventory.ini"
}
