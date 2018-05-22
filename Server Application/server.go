package main

import (
	"github.com/gin-contrib/cors"
	"github.com/gin-contrib/static"
	"github.com/gin-gonic/gin"
	"fmt"
	"net/http"
	"io/ioutil"
	"encoding/json"
	"os"
	"strings"
	"bufio"
	"time"
)

type Position struct{
	Datetime string `json:"Datetime" binding:"required"`
	Data []float32 `json:"Data" binding:"required"`
}

type Acceleration struct{
	Datetime string `json:"Datetime" binding:"required"`
	Data []float32 `json:"Data" binding:"required"`
}

type Compass struct{
	Datetime string `json:"Datetime" binding:"required"`
	Data []float32 `json:"Data" binding:"required"`
}

type Data struct {
	ExperimentName string `json:"Experiment Name" binding:"required"`
	SubmissionDatetime string `json:"SubmissionDatetime" binding:"required"`
	ConfigurationFile string `json:"Configuration File" binding:"required"`
	BeaconLabel string `json:"Beacon Label" binding:"required"`
	BeaconHeight float32 `json:"Beacon Height" binding:"required"`
	PositionLog []Position `json:"PositionLog" binding:"required"`
	AccelerometerData []Acceleration `json:"AccelerometerData" binding:"required"`
	CompassData []Compass `json:"CompassData" binding:"required"`
}

func main() {
	r := initRouter()
	fmt.Println(join())
	r.Run(":5000")
}

func initRouter() *gin.Engine {

	r := gin.Default()

	//Loads static files
	r.Use(static.Serve("/", static.LocalFile("files", true)))

	r.Use(cors.Default())

	r.GET("/results", func(c *gin.Context){
		c.Status(http.StatusNoContent)
	})

	r.POST("/results/:expName", func(c *gin.Context){
		experimentName := c.Param("expName")

		//Read JSON data from request body
		x, _ := ioutil.ReadAll(c.Request.Body)
		var d Data
		err := json.Unmarshal(x, &d)

		//Check if JSON data is valid
		if err == nil{
			//Check if file already exists
			dir, _ := os.Getwd()
			if _, err := os.Stat(join(dir, "/data/", experimentName, "_position.csv")); err == nil{
				c.JSON(http.StatusConflict, gin.H{"Success": false,"Error": "File already exists"})
			} else{
				//Write the data to csv
				if checkData(d){
					writeCSV("position", d, experimentName)
					writeCSV("acceleration", d, experimentName)
					writeCSV("config", d, experimentName)
					writeCSV("compass", d, experimentName)
					c.JSON(http.StatusCreated, gin.H{"Success": true})
				} else{
					c.JSON(http.StatusPreconditionFailed, gin.H{"Success": true, "Error": "Empty values"})
				}
			}
		}else{
			//Error parsing JSON data
			fmt.Println(err);
			c.JSON(http.StatusBadRequest, gin.H{"Success": false,"Error": err})
		}
	})

	return r
}

//Concatinates strings
func join(strs ...string) string {
	var sb strings.Builder
	for _, str := range strs {
		sb.WriteString(str)
	}
	return sb.String()
}

//Checks whether or not error occured
func check(err error){
	if err != nil{
		panic(err)
	}
}

//Checks JSON data structure to ensure no fields are empty
func checkData(d Data) bool{
	if d.ExperimentName == ""{
		return false
	} else if d.SubmissionDatetime == ""{
		return false
	} else if d.ConfigurationFile == ""{
		return false
	} else if d.BeaconLabel == ""{
		return false
	} else if d.BeaconHeight < 0{
		return false
	} else if d.PositionLog == nil{
		return false
	} else if d.AccelerometerData == nil{
		return false
	}

	for _, point := range d.PositionLog{
		if point.Datetime == ""{
			return false;
		} else if len(point.Data) != 2{
			return false
		}
	}

	for _, point := range d.AccelerometerData{
		if point.Datetime == ""{
			return false;
		} else if len(point.Data) != 3{
			return false
		}
	}

	return true
}

//Outputs data to csv file, for "position", "accelerometer", and "config" data
func writeCSV(csvType string, d Data, experimentName string) bool{
	t := time.Now()
	dir, _ := os.Getwd()
	file, fErr := os.Create(join(dir, "/data/", experimentName, "_", csvType, ".csv"))
	check(fErr)
	
	writer := bufio.NewWriter(file)
	if csvType == "position"{
		fmt.Fprintf(writer, "\"Datetime\",\"realx\",\"realy\"")
		for _, data := range d.PositionLog{
			fmt.Fprintf(writer,"\n\"%s\",%f,%f", data.Datetime, data.Data[0], data.Data[1])
		}
	} else if csvType == "acceleration"{
		fmt.Fprintf(writer, "\"Datetime\",\"realx\",\"realy\"")
		for _, data := range d.AccelerometerData{
			fmt.Fprintf(writer,"\n\"%s\",%f,%f,%f", data.Datetime, data.Data[0], data.Data[1], data.Data[2])
		}
	} else if csvType == "compass"{
		fmt.Fprintf(writer, "\"Datetime\",\"azimuth\",\"magneticField\"")
		for _, data := range d.CompassData{
			fmt.Fprintf(writer,"\n\"%s\",%f,%f", data.Datetime, data.Data[0], data.Data[1])
		}
	} else if csvType == "config"{
		fmt.Fprintf(writer, "{\n")
		fmt.Fprintf(writer, "\t\"SubmissionDatetime\": \"%s\"\n", d.SubmissionDatetime)
		fmt.Fprintf(writer, "\t\"ReceiveDatetime\": \"%s\"\n", t.Format(time.RFC3339))
		fmt.Fprintf(writer, "\t\"Experiment Name\": \"%s\"\n", d.ExperimentName)
		fmt.Fprintf(writer, "\t\"Configuration File\": \"%s\"\n", d.ConfigurationFile)
		fmt.Fprintf(writer, "\t\"Beacon Label\": \"%s\"\n", d.BeaconLabel)
		fmt.Fprintf(writer, "\t\"Beacon Height\": %f\n", d.BeaconHeight)
		fmt.Fprintf(writer, "}")
	}
	writer.Flush()
	file.Close()
	return true
}