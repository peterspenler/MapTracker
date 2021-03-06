package main

import (
	"bufio"
	"flag"
	"fmt"
	"github.com/gin-contrib/cors"
	"github.com/gin-contrib/static"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type Position struct {
	Datetime string    `binding:"required"`
	Data     []float32 `binding:"required"`
	Paused   int       `binding:"required"`
}

type Acceleration struct {
	Datetime string    `binding:"required"`
	Data     []float32 `binding:"required"`
}

type Compass struct {
	Datetime string    `binding:"required"`
	Data     []float32 `binding:"required"`
}

type Data struct {
	ExperimentName     string         `json:"Experiment Name" binding:"required"`
	SubmissionDatetime string         `binding:"required"`
	ConfigurationFile  string         `json:"Configuration File" binding:"required"`
	BeaconLabel        string         `json:"Beacon Label" binding:"required"`
	BeaconHeight       float32        `json:"Beacon Height" binding:"required"`
	PositionLog        []Position     `binding:"required"`
	AccelerometerData  []Acceleration `binding:"required"`
	CompassData        []Compass      `binding:"required"`
}

func main() {
	port := flag.String("port", "32680", "Port number to use for this service")
	key := flag.String("key", "", "Key file to use")
	crt := flag.String("crt", "", "Crt file to use")
	flag.Parse()
	r := initRouter()
	s := &http.Server{
		Addr:    ":" + *port,
		Handler: r,
	}
	log.Fatal(s.ListenAndServeTLS(*crt, *key))

}

func initRouter() *gin.Engine {

	r := gin.Default()

	//Loads static files
	r.Use(static.Serve("/", static.LocalFile("files", true)))

	r.Use(cors.Default())

	r.GET("/results", func(c *gin.Context) {
		c.Status(http.StatusNoContent)
	})

	r.POST("/results/:expName", func(c *gin.Context) {
		experimentName := c.Param("expName")

		var d Data
		if err := c.BindJSON(&d); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"Success": false, "Error": "Json Invalid"})
			return
		}
		dir, _ := os.Getwd()
		experimentName = time.Now().Format(time.RFC3339) + "_" + experimentName
		if _, err := os.Stat(dir + "/data/" + experimentName + "_" + d.BeaconLabel + "_position.csv"); err == nil {
			c.JSON(http.StatusConflict, gin.H{"Success": false, "Error": "File already exists"})
			return
		}
		// Write the data to csv
		if !checkData(d) {
			c.JSON(http.StatusPreconditionFailed, gin.H{"Success": true, "Error": "Empty values"})
			return
		}

		writeCSV(CSV_POSITION, d, experimentName)
		writeCSV(CSV_ACCELERATION, d, experimentName)
		writeCSV(CSV_COMPASS, d, experimentName)
		writeCSV(CSV_CONFIG, d, experimentName)
		c.JSON(http.StatusCreated, gin.H{"Success": true})
	})

	return r
}

// Panics if error occured
func check(err error) {
	if err != nil {
		panic(err)
	}
}

//Checks JSON data structure to ensure no fields are empty
func checkData(d Data) bool {
	if d.ExperimentName == "" ||
		d.SubmissionDatetime == "" ||
		d.ConfigurationFile == "" ||
		d.BeaconLabel == "" ||
		d.BeaconHeight < 0 ||
		d.PositionLog == nil ||
		d.AccelerometerData == nil {
		return false
	}

	for _, point := range d.PositionLog {
		if point.Datetime == "" || len(point.Data) != 2 {
			return false
		}
	}

	for _, point := range d.AccelerometerData {
		if point.Datetime == "" || len(point.Data) != 3 {
			return false
		}
	}

	return true
}

const (
	CSV_POSITION = iota
	CSV_ACCELERATION
	CSV_COMPASS
	CSV_CONFIG
)

//Outputs data to csv file, for "position", "accelerometer", and "config" data
func writeCSV(csvType int, d Data, experimentName string) bool {
	t := time.Now()
	dir, _ := os.Getwd()

	var csvname string
	switch csvType {
	case CSV_POSITION:
		csvname = "position"
	case CSV_ACCELERATION:
		csvname = "acceleration"
	case CSV_COMPASS:
		csvname = "compass"
	case CSV_CONFIG:
		csvname = "config"
	}
	fpath := dir + "/data/" + experimentName + "_" + d.BeaconLabel + "_" + csvname + ".csv"
	fpath = filepath.Clean(fpath)
	fpath, err := filepath.Abs(fpath)
	if err != nil || !strings.HasPrefix(fpath, dir+"/data") {
		log.Println("Prefix wrong", fpath, dir)
		return false
	}
	file, fErr := os.Create(fpath)
	check(fErr)

	writer := bufio.NewWriter(file)
	if csvType == CSV_POSITION {
		fmt.Fprintf(writer, "\"Datetime\",\"realx\",\"realy\",\"paused\"")
		for _, data := range d.PositionLog {
			fmt.Fprintf(writer, "\n\"%s\",%f,%f,%d", data.Datetime, data.Data[0], data.Data[1], data.Paused)
		}
	} else if csvType == CSV_ACCELERATION {
		fmt.Fprintf(writer, "\"Datetime\",\"realx\",\"realy\",\"realz\"")
		for _, data := range d.AccelerometerData {
			fmt.Fprintf(writer, "\n\"%s\",%f,%f,%f", data.Datetime, data.Data[0], data.Data[1], data.Data[2])
		}
	} else if csvType == CSV_COMPASS {
		fmt.Fprintf(writer, "\"Datetime\",\"azimuth\",\"magneticField\"")
		for _, data := range d.CompassData {
			fmt.Fprintf(writer, "\n\"%s\",%f,%f", data.Datetime, data.Data[0], data.Data[1])
		}
	} else if csvType == CSV_CONFIG {
		// This should be converted to a struct the uses the json package but for now use comma
		fmt.Fprintf(writer, "{\n")
		fmt.Fprintf(writer, "\t\"SubmissionDatetime\": \"%s\",\n", d.SubmissionDatetime)
		fmt.Fprintf(writer, "\t\"ReceiveDatetime\": \"%s\",\n", t.Format(time.RFC3339Nano))
		fmt.Fprintf(writer, "\t\"Experiment Name\": \"%s\",\n", d.ExperimentName)
		fmt.Fprintf(writer, "\t\"Configuration File\": \"%s\",\n", d.ConfigurationFile)
		fmt.Fprintf(writer, "\t\"Beacon Label\": \"%s\",\n", d.BeaconLabel)
		fmt.Fprintf(writer, "\t\"Beacon Height\": %f\n", d.BeaconHeight)
		fmt.Fprintf(writer, "}")
	}
	writer.Flush()
	file.Close()
	return true
}
