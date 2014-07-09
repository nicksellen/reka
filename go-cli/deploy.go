package main

import (
  "net/http"
  "fmt"
  "os"
  "net/url"
  "log"
  "bytes"
  "encoding/json"
)


func main() {

  spec := os.Args[1]

  resp, err := http.PostForm("http://localhost:5100/apps", url.Values{"spec": { spec } })

  //fmt.Println(resp.StatusCode)
  //fmt.Println(resp.Header)

  defer resp.Body.Close()

  body := &bytes.Buffer{}

  _, err = body.ReadFrom(resp.Body)
  if err != nil {
    log.Fatal(err)
  }

  //var data interface{}
  var data map[string]interface{}

  err = json.Unmarshal(body.Bytes(), &data)

  fmt.Println(data["message"])

}
