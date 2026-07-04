output "master_ip" {
  description = "Indirizzo IP del nodo Master"
  value       = multipass_instance.master.ipv4
}

output "worker_ip" {
  description = "Indirizzo IP del nodo Worker"
  value       = multipass_instance.worker.ipv4
}
