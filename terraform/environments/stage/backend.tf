terraform { 
  cloud { 
    
    organization = "ingesoffttt" 

    workspaces { 
      name = "ecommerce-stage" 
    } 
  } 
}