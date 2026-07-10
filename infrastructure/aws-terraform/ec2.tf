# AWS COMPUTE INFRASTRUCTURE (EC2)

# provisiona le macchine virtuali su cui girerà Kubernetes (K3s).
# Implementa la creazione di chiavi SSH in modo dinamico e definisce i due nodi:
# - k3s_master (t3.small): Gestisce il control plane del cluster.
# - k3s_worker (t3.micro): Esegue fisicamente il container Spring Boot.
# Usa local_file per salvare chiavi e IP in locale per permetterne l'uso ad Ansible.

data "aws_ami" "ubuntu" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  owners = ["099720109477"] # Canonical
}

# Genera chiave SSH dinamicamente per Ansible
resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "generated_key" {
  key_name   = "maps-k3s-key"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "aws_instance" "k3s_master" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.small"
  subnet_id     = aws_subnet.public_1.id
  vpc_security_group_ids = [aws_security_group.k3s_nodes.id]
  key_name      = aws_key_pair.generated_key.key_name
  associate_public_ip_address = true

  tags = {
    Name = "maps-k3s-master"
  }
}

resource "aws_instance" "k3s_worker" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"
  subnet_id     = aws_subnet.public_2.id
  vpc_security_group_ids = [aws_security_group.k3s_nodes.id]
  key_name      = aws_key_pair.generated_key.key_name
  associate_public_ip_address = true

  tags = {
    Name = "maps-k3s-worker"
  }
}

# Genera il file inventory per Ansible basato sul template
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/../ansible/inventory.tmpl", {
    master_ip = aws_instance.k3s_master.public_ip
    worker_ip = aws_instance.k3s_worker.public_ip
  })
  filename = "${path.module}/../ansible/inventory.ini"
}

# Salva la chiave SSH in locale (ignorato da git) per l'uso da parte di Ansible e GitHub Actions
resource "local_file" "private_key" {
  content  = tls_private_key.ssh_key.private_key_pem
  filename = "${path.module}/../ansible/k3s-key.pem"
  file_permission = "0600"
}
