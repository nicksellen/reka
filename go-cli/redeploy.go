package main

import (
  "net/http"
  "fmt"
  "log"
  "os"
)


func main() {

  client := &http.Client{}  

  uuid := os.Args[1]

  url := fmt.Sprintf("http://reka:81/apps/%s/redeploy", uuid)

  req, err := http.NewRequest("PUT", url, nil) 

  if err != nil {
    log.Fatal(err)
  }

  _, err = client.Do(req)

  fmt.Printf("redeployed %s!\n", uuid)

}