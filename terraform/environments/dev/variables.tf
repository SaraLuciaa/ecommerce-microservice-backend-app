variable "do_token" {
  description = "DigitalOcean API Token"
  type        = string
  sensitive   = true
}

variable "cluster_name" {
  description = "Name of the Kubernetes cluster"
  type        = string
  default     = "k8s-1-32-10-do-0-nyc1-1764540863469"
}
# Note: Cluster was manually created in DigitalOcean with this name

variable "region" {
  description = "DigitalOcean region for the cluster"
  type        = string
  default     = "nyc1"
}

variable "node_size" {
  description = "Size of the worker nodes"
  type        = string
  default     = "s-2vcpu-4gb"
}

variable "node_count" {
  description = "Number of worker nodes"
  type        = number
  default     = 3
}

variable "node_pool_name" {
  description = "Name of the node pool"
  type        = string
  default     = "pool-bw7wtgk9a"
}

variable "k8s_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.32.10-do.0"
}
