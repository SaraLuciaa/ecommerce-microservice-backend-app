variable "name" {
  description = "Name of the MySQL service"
  type        = string
  default     = "mysql-db"
}

variable "root_password" {
  description = "MySQL root password"
  type        = string
  default     = "root" # In production, use secrets!
}

variable "database_name" {
  description = "Initial database name"
  type        = string
  default     = "ecommerce_db"
}
