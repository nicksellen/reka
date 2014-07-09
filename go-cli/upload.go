package main

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

func main() {

	root := os.Args[1]

	fmt.Printf("uploading files from %s\n", root)

	var wg sync.WaitGroup

	walkFunc := func(path string, info os.FileInfo, err error) error {

		if strings.HasPrefix(info.Name(), ".") && len(info.Name()) > 1 {
			if (info.IsDir()) {
				fmt.Printf("skipping %s\n", path)
				return filepath.SkipDir
			} else {
				fmt.Printf("skipping %s\n", path)
				return nil
			}
		}

		if !info.IsDir() {

			wg.Add(1)

			go func(path string){

				defer wg.Done()
				defer fmt.Printf(" %s\n", path)

				url := fmt.Sprintf("http://localhost:5100/files/%s", path)

				req, err := newfileUploadRequest(url, path)

				if err != nil {
					log.Fatal(err)
				}
				client := &http.Client{}
				resp, err := client.Do(req)
				if err != nil {
					log.Fatal(err)
				} else {
					body := &bytes.Buffer{}
					_, err := body.ReadFrom(resp.Body)
					if err != nil {
						log.Fatal(err)
					}
					resp.Body.Close()
					//fmt.Println(resp.StatusCode)
					//fmt.Println(resp.Header)
					//fmt.Println(body)
				}

			}(path)
		}

		return nil
	}

	filepath.Walk(root, walkFunc)

	wg.Wait()

}

func newfileUploadRequest(uri string, path string) (*http.Request, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)
	part, err := writer.CreateFormFile("file", filepath.Base(path))
	if err != nil {
		return nil, err
	}
	_, err = io.Copy(part, file)

	err = writer.Close()
	if err != nil {
		return nil, err
	}
	req, err := http.NewRequest("POST", uri, body)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	return req, err
}
