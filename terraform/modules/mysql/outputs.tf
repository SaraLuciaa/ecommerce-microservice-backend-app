output "service_name" {
  value = kubernetes_service.mysql.metadata[0].name
}

output "port" {
  value = kubernetes_service.mysql.spec[0].port[0].port
}
