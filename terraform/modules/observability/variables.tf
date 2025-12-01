variable "namespace" {
  description = "Namespace for monitoring tools"
  type        = string
  default     = "monitoring"
}

variable "grafana_password" {
  description = "Password for Grafana admin user"
  type        = string
  sensitive   = true
}
