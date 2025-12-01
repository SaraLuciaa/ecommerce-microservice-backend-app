variable "do_token" {
  type        = string
  sensitive   = true
}

variable "cluster_name" {
  type = string
}

variable "namespace" {
  type = string
}

variable "env" {
  type = string
}

variable "docker_tag" {
  type = string
}

variable "replicas" {
  type    = number
  default = 1
}