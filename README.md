# ALPACA
ALPACA is an app reviews opinions mining tool based on topics and intentions.  
Copyright (C) 2017-2018 SIS Lab, Auburn University

This program is free software. 

Contact: lenniel@auburn.edu (Phong Vu - Auburn University)
  
(Video demo)
* [Video Demo](https://www.youtube.com/watch?v=UNw573qtEpw)

(Results - view by downloading the html file and run it with your browser)
* [examples result 1](https://github.com/phong1990/ALPACA/blob/master/res/examples/requests_songs_moderate.html)
* [examples result 2](https://github.com/phong1990/ALPACA/blob/master/res/examples/complaint_songs_moderate.html)
* [examples result 3](https://github.com/phong1990/ALPACA/blob/master/res/examples/request_songs_flexible.html)

(Tool and technical paper - in the work, but you can read it for technical reference)

*[Tool paper](https://github.com/phong1990/ALPACA/blob/master/res/papers/ALPACA-tool-v1.pdf)
*[Technical Paper](https://github.com/phong1990/ALPACA/blob/master/res/papers/mining-user-opinions-WORK_IN_PROGRESS.pdf)

## Introduction
**ALPACA** is a tool for extracting user opinions from app review using topics and intentions. It has some of the core functions of MARK (2016) and Phrase-based (2017) tools, while also providing a new novel function to extract opinions.
## System requirement
Minimum requirement
  Windows 7 (Windows 10 is bad for datamining in general)
  RAM: 12gb (more ram if you plan to run more than 3 millions reviews)
  CPU: intel i5
  
## Installation
1. Download the running package file ([ALPACARunningPackage.zip](http://www.mediafire.com/file/cl3lu0k75kxl344/ALPACARunningPackage.zip))
2. Unzip the package.
3. Run ALPACA.jar (no installation required)
4. (OPTIONAL) download 3 millions reviews. (Link available soon) (see citation instruction 2)
5. (OPTIONAL) download custom intention patterns. [Comparison Intention](https://github.com/phong1990/ALPACA/blob/master/res/seedPatterns/newComparisons.csv)
## Usage
### Extracting opinions
1a. If you have the folder that contains compatible data for ALPACA, you can choose it as your "Data Folder"

![Step 1](https://github.com/phong1990/ALPACA/blob/master/res/img/step1.png)

1b. If you only have raw review data, first you will need to convert it into our .csv format (UTF-8, default separator). An example of the csv file can be found in the running package. Click on the "Import CSV" button to import it to an empty data folder of your choice. You only have to import once, next time you only need step 1a.

![Step 1b](https://github.com/phong1990/ALPACA/blob/master/res/img/step1-data.png)

2. Now that you have the data folder, you will need to provide the config file for the Text Normalizer module. This config file will apply debug mode and direct ALPACA to the correct location of the dictionary folder in your computer. An example of the config file is inside the dictionary folder of our running package. You will need to change the directory path in there before running ALPACA.

![Step 2](https://github.com/phong1990/ALPACA/blob/master/res/img/step2.png)

3. Preprocessing: This step is required for ALPACA to analyze your data, and only needed to be done once. Please choose both word2vec training and pattern learning unless you know what you are doing (e.g. you have a better word2vec file from somewhere else, or you provide your own patterns). The artifacts produced by those options are vital to the next steps.

![Step 3](https://github.com/phong1990/ALPACA/blob/master/res/img/step3.png)

4. Extrating Keywords: This is straight forward. The result is a csv file with different ranking schemas for all keywords found in your data. This is similar to keyword ranking and extraction of MARK (see citation instruction 3). DO NOT SKIP THIS STEP unless you know what you are doing (e.g. providing your own keyword ranking for your research)

![Step 4](https://github.com/phong1990/ALPACA/blob/master/res/img/keywords.png)

5. Expanding topic (OPTIONAL): This option allow you to explore the topic you are interested about by providing keywords you think related to that topic. ALPACA will analyze those keywords, expand them into a bigger relevant set of keywords and show you the collective summary of the topic in phrase format. This result is the same as our Phrase-based tool's (see citation instruction 3).

![Step 5.1](https://github.com/phong1990/ALPACA/blob/master/res/img/step4.png)

Expanded topic words

![Step 5.2](https://github.com/phong1990/ALPACA/blob/master/res/img/step4-words.png)

Expanded topic descriptions

![Step 5.3](https://github.com/phong1990/ALPACA/blob/master/res/img/step4-description.png)

6. Expanding Intent Pattern (RECOMMENDED): This option is not required, but it would greatly improve the result of ALPACA by expanding your patterns using the data from your reviews. There are two default intentions: Requests and Complaints. However, you can make your own patterns as in our comparison.csv example in the running package. The pattern format has to contain at least a functional word (like the words in ALPACARunningPackage\dictionary\baseWord\misc\) and at least a POS tag (from  [Penn Tree Bank site](https://catalog.ldc.upenn.edu/docs/LDC99T42/tagguid1.pdf) ). You can define different intentions based on your interest and find more similar patterns from you data. Remember to set your threshold of how similar the patterns need to be to the original patterns.

![Step 6](https://github.com/phong1990/ALPACA/blob/master/res/img/step5.png)

The mined patterns:

![Step 6patt](https://github.com/phong1990/ALPACA/blob/master/res/img/patterms.png)

7. Extracting opinions: An opinion is a phrase that match an intention pattern and describe the topic of interest. You can import the keywords set from step 5 and the pattern set expanded on step 6 (or use our default intentions, but the results will not be as good since ALPACA is data-driven). Please remember to choose a threshold for this step as it would directly affect the final result. The result is an .html file with all the opinions found.

![Step 7](https://github.com/phong1990/ALPACA/blob/master/res/img/step6.png)

This is how the final results should look like:

![result](https://github.com/phong1990/ALPACA/blob/master/res/img/results.png)



### Sumarizing topics (Phrase-based 2017):
  read step 5 above.

### Analyzing keywords (MARK 2016):
  read step 4 and 5. 
  We will add the interface of MARK in the future.
  
## Data
1. Our vocabulary data is stored under ALPACARunningPackage\dictionary\baseWord. The folder contains:
1.1. \dictionary\misc: all functional words. You can add more words of interest to domain.txt. Those words will be used to define patterns. (like "fix" is a common word in reviews, often indicating a problem needed fixing.)
1.2. \dictionary\wordnet: contains all the words from wordnet 3.0
1.3. \dictionary\newword: contains the words that are not from wordnet 3.0, or not well defined (such as lacking verb/noun/adj forms, or lacking irregular variants)
2. The \dictionary\correctorTraining folder is a text corpus from wikipedia, used to improve the accuracy of our spelling corrector. You can change to other text corpus if you want to.
3. \dictionary\edu folder contain a Stanford NLP tagger for English.
4. \dictionary\improvised has some words, which I don't remember what they are for, but I'm too afraid to delete them and risk breaking the program.
5. \dictionary\Map: contains the correct words for common mistakes in reviews. I have discussed about this in the MARK paper. Overtime, I will add more and more common errors to this map with new data.
6. \dictionary\stop: those are the stop words. 
7. \dictionary\trigramData: this is a trained model for our spellCorrector. Do not delete or modify anything from it. We trained it with a few billions lines of text so it's likely not not ever get an update.
8. \dictionary\config.ini: this file is for locating the dictionary folder in ALPACA. You need to change the path inside to the correct location in your computer.
9. \aditionalText: this is the processed text of 3 millions reviews. You can use it as a supplement text corpus for training word2vec for your data if you don't have too many reviews. It helps the deeplearning process understand the relationships between words better in the context of mobile app. Therefore, the more reviews you have, the better ALPACA will be able to understand it.
10. \replication: this has around 13k reviews of Magic Tiles to replicate our experiments and for demonstrate the usage of ALPACA. You can use it to learn how to use our tool.

## Citation instructions
1. We will soon publish the papers for ALPACA on arXiv, please come back later.
2. If you are using the additional dataset (3 millions reviews) please cite our paper "Mining user opinions in mobile app reviews: A keyword-based approach (t)".
3. If you want to use the topic and phrase based analysis feature, please refer to our papers "Mining user opinions in mobile app reviews: A keyword-based approach (t)" AND "Phrase-based extraction of user opinions in mobile app reviews"
