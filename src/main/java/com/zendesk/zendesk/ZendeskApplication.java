package com.zendesk.zendesk;

import com.fasterxml.jackson.databind.util.JSONPObject;


import org.json.JSONArray;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files


@SpringBootApplication
public class ZendeskApplication {

	public static RestTemplate restTemplate = new RestTemplate();
	private static Statement statement;



	/**
	 * Recursion method to extract all visitors from chats
	 *
	 * @param  cursor  param for the next page
	 * @return      Save visitors id and phone in data.txt file
	 */
	public static String getChatList(String cursor) throws JSONException, IOException {

		String Url = "https://www.zopim.com/api/v2/chats?cursor="+cursor;
		BufferedWriter writer = new BufferedWriter(new FileWriter("data.txt",true));

		String token_value = "wSSSrIHetm4PtstTR2Xtcg5jIFJXiyLTjHxc7R7KwJMONK8F0yMWQPZgN9xOaP78";
		//setting the headers
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token_value);

		HttpEntity entity = new HttpEntity(headers);

		//executing the GET call
		HttpEntity<String> response = restTemplate.exchange(Url, HttpMethod.GET, entity, String.class);

		String jsonStr = response.getBody();
		JSONObject jsonObj = new JSONObject(jsonStr);

		//extracting data array from json string
		JSONArray chatsDetails = jsonObj.getJSONArray("chats");
		int length = chatsDetails .length();


		//loop to get all json objects from chats json array
		for(int i=0; i<length; i++)
		{
			JSONObject jObj = chatsDetails.getJSONObject(i);
			// getting inner array visitors
			JSONObject visitorDetails = chatsDetails.getJSONObject(i);
			// getting json objects from visitor json array
			JSONObject json = visitorDetails.getJSONObject("visitor");
			//System.out.println(json.toString());
			if(json.getString("phone").toString().startsWith("+")){
				writer.write(json.getString("id").toString()+","+json.getString("phone").toString()+"\n");
			}
		}
		//next page
		String next_url = jsonObj.getString("next_url");
		if(next_url.isEmpty()){
			writer.write(cursor+"\n");
			System.exit(0);
		}
		cursor = next_url.split("=")[1];

		writer.close();
		return getChatList( cursor);
	}


	/**
	 * This method will update the visitor phone number in Zensesk
	 *
	 * @param  userId  user id in YC database
	 * @param  visitorId  user id in Zendesk database
	 */
	public static void updateVisitorPhone(String visitorId, int userId) throws JSONException, IOException {

		String Url = "https://www.zopim.com/api/v2/visitors/"+visitorId;

		String token_value = "";
		//setting the headers
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + token_value);
		headers.add("Content-Type", "application/json");
		HttpEntity entity = new HttpEntity(headers);

		JSONObject reqDataJsonObject = new JSONObject();
		reqDataJsonObject.put("phone", userId+"");

		//executing the GET call
		HttpEntity<String> requestEntity = new HttpEntity<String>(reqDataJsonObject.toString(), headers);

		ResponseEntity<String> response = restTemplate.exchange(Url, HttpMethod.PUT, requestEntity, String.class);

		String jsonStr = response.getBody();
		JSONObject jsonObj = new JSONObject(jsonStr);

		System.out.println("User "+jsonObj.getString("id")+" updated");

	}

	/**
	 * This method will create mySql connection statement
	 *
	 * @return  stmt  connection statement
	 * @note  please change database username and password
	 */
	public static Statement MysqlCon(){
		Statement stmt = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con= DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/insurance_db","root","root");
			stmt=con.createStatement();
		}catch(Exception e){ System.out.println(e);}

		return stmt;
	}



	/**
	 * This method will select user by phone number in YC database
	 *
	 * @return  userId  return the user id
	 */
	public static int getuserID(String userPhone) throws SQLException {

		int userId = -1;
		String prest= "SELECT id FROM user  WHERE mobile="+userPhone+" AND country_id=1";
		ResultSet rs=statement.executeQuery(prest);
		while (rs.next()){
			userId = rs.getInt(1);
		}
		return userId;
	}

	/**
	 * This method will get the user phone from data.txt file ,
	 * find this user id in YC database
	 * then replace zendesk visitor phone number with user id
	 */
	public static void setVisitorsPhone() {
		try {
			File myObj = new File("data.txt");
			Scanner myReader = new Scanner(myObj);
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				String visitorId = data.split(",")[0];
				String userPhone = data.split(",")[1].substring(4);
				//System.out.println(userPhone);
				int userId = getuserID(userPhone);

				//if user exist in YC we update Sendesk visitor phone number
				if(userId!=-1) {
					updateVisitorPhone(visitorId, userId);
				}
			}
			myReader.close();
		} catch (SQLException | JSONException | IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws JSONException, IOException {

		SpringApplication.run(ZendeskApplication.class, args);
		System.out.println("Lets Go");



		//Get Visitors id and phone from chats

		//String cursor = "";
		//getChatList(cursor);

		//remove duplicate result using
		//https://www.textmechanic.co/Remove-Duplicate-Lines.html

		// Connect to database
		statement = MysqlCon();

		//Find User in YC database by phone then update zendesk visitor
		setVisitorsPhone();

	}

}
