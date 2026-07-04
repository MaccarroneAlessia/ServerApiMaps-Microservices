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
