package org.beckyandnathan.www;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@SuppressWarnings("serial")
public class HotelAvailabilityChecker extends HttpServlet {

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.103 Safari/537.36";
	private static final int TIMEOUT_SECONDS = 20;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		try {
			Calendar c = Calendar.getInstance();

			c.set(2014, Calendar.SEPTEMBER, 7);
			this.checkDate(c.getTime(), 5, false);

			c.set(2014, Calendar.SEPTEMBER, 8);
			this.checkDate(c.getTime(), 5, false);

			c.set(2014, Calendar.SEPTEMBER, 9);
			this.checkDate(c.getTime(), 5, false);

			c.set(2015, Calendar.JUNE, 8);
			this.checkDate(c.getTime(), 5, true);
			
		} catch (MessagingException e) {
			// TODO Log an error
			e.printStackTrace();
		}
	}

	private void checkDate(Date arrivalDate, int numNights,
			boolean expectedAvailability) throws IOException,
			MessagingException {

		if (this.isBucutiAvailable(arrivalDate, numNights) != expectedAvailability) {
			this.sendNotificationEmail("Bucuti", arrivalDate, numNights, !expectedAvailability);
		}
		
		//if (this.isMancheboAvailable(arrivalDate, numNights) != expectedAvailability) {
		//	this.sendNotificationEmail("Manchebo Beach", arrivalDate,
		//			numNights, !expectedAvailability);
		//}
	}

	private boolean isMancheboAvailable(Date arrivalDate, int numNights)
			throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(arrivalDate);
		cal.add(Calendar.DAY_OF_MONTH, numNights);
		Date departureDate = cal.getTime();

		Document doc = Jsoup
				.connect(
						"https://book.b4checkin.com/manchebo/widget_redirect.asp")
				.timeout(TIMEOUT_SECONDS * 1000)
				.userAgent(USER_AGENT)
				.referrer("http://www.manchebo.com/Aruba-Beach-Resort.html")
				.header("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.8")
				.data("arrivalDate",
						new SimpleDateFormat("MM/dd/yyyy").format(arrivalDate))
				.data("ArvDD", (new SimpleDateFormat("dd")).format(arrivalDate))
				.data("ArvMM", (new SimpleDateFormat("MM")).format(arrivalDate))
				.data("ArvYY",
						(new SimpleDateFormat("yyyy")).format(arrivalDate))
				.data("departureDate",
						new SimpleDateFormat("MM/dd/yyyy")
								.format(departureDate))
				.data("DepDD",
						(new SimpleDateFormat("dd")).format(departureDate))
				.data("DepMM",
						(new SimpleDateFormat("MM")).format(departureDate))
				.data("DepYY",
						(new SimpleDateFormat("yyyy")).format(departureDate))
				.post();
		String html = doc.html();

		cal.setTime(arrivalDate);
		int endNight = cal.get(Calendar.DAY_OF_MONTH) + numNights;

		for (int d = cal.get(Calendar.DAY_OF_MONTH); d < endNight; d++) {
			String p = "month1TotalAvail\\[0\\]\\[" + d + "\\] = \"([0-9])\"";
			Pattern lookFor = Pattern.compile(p);
			Matcher m = lookFor.matcher(html);

			if (m.find()) {
				int numRoomTypesAvailable = Integer.parseInt(m.group(1));
				if (numRoomTypesAvailable == 0) {
					return false;
				}
			} else {
				// TODO Throw an exception. This should "never" happen. In
				// reality, this will happen if the arrivalDate and
				// departureDate are in different months. But we don't care
				// about that for now
			}
		}

		// We never came across a night with 0 room types available
		return true;
	}

	private boolean isBucutiAvailable(Date arrivalDate, int numNights)
			throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
		Random generator = new Random();
		int x = generator.nextInt(35) + 1;
		int y = generator.nextInt(11) + 1;
		String url = "https://booking.ihotelier.com/istay/istay.jsp?HotelID=95144&LanguageID=en&Rooms=1&DateIn="
				+ URLEncoder.encode(dateFormat.format(arrivalDate), "UTF-8")
				+ "&Length=" + numNights + "&Adults=2&x=" + x + "&y=" + y;
		Document doc = Jsoup
				.connect(url)
				.timeout(TIMEOUT_SECONDS * 1000)
				.userAgent(USER_AGENT)
				.referrer("http://www.bucuti.com/")
				.header("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.8").get();
		return !doc.body().attr("id").equals("select-dates");
	}

	private void sendNotificationEmail(String hotelName, Date arrivalDate,
			int numNights, boolean isAvailable)
			throws UnsupportedEncodingException, MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String subject = hotelName + " has " + (isAvailable ? " " : "NO ")
				+ "availability "
				+ (new SimpleDateFormat("M/d/yyyy")).format(arrivalDate)
				+ " for " + numNights + " nights";

		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("ndbeach@gmail.com", "Nathan Beach"));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
				"ndbeach@gmail.com", "Nathan Beach"));
		msg.setSubject(subject);
		msg.setText("...");
		Transport.send(msg);
	}
}
