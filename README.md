# StructuredStreamingMongoDB

This project focuses on end-to-end streaming data processing using Spark Structured Streaming and storing the result into MongoDB.

A high level overview of the project would be:
	a. Kafka Producer will read data from a streaming source and publish it into a Kafka Topic.
	b. Read data from Kafka Topic in streaming fashion using Spark Structured Streaming.
	c. Perform some basic transformation on the data using DataFrame.
	d. Push to data into MongoDB as micro-batches of data.
	e. Data would be read by Kafka Producer from "http://stream.meetup.com/2/rsvps", which continuously publishes data. 

Following are the version of required language/technologies:
	a. Scala - 2.11.12
	b. Apache Spark - 2.4.4
	c. MongoDB - 3.6.8
	d. Apache Kafka - 2.2.0

*Note: You need to pass `dev` as Program Arguments while running the program.
