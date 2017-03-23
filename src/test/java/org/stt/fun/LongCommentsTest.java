/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.fun;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;

/**
 *
 * @author dante
 */
public class LongCommentsTest extends AchievementTestBase {

	private LongComments sut;

	@Before
	public void setup() {
		sut = new LongComments(resourceBundle, 3, 5);
		sut.start();
	}

	@Test
	public void shouldNotTriggerIfNotEnoughTimes() {
		// GIVEN
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.done();

		// WHEN
		boolean achieved = sut.isAchieved();

		// THEN
		Assert.assertThat(achieved, is(false));
	}

	@Test
	public void shouldNotTriggerIfCommentTooShort() {
		// GIVEN
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("1234", LocalDateTime.now()));
		sut.done();

		// WHEN
		boolean achieved = sut.isAchieved();

		// THEN
		Assert.assertThat(achieved, is(false));
	}

	@Test
	public void shouldTriggerIfEnoughLongComments() {
		// GIVEN
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("123456", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("1234567", LocalDateTime.now()));
		sut.done();

		// WHEN
		boolean achieved = sut.isAchieved();

		// THEN
		Assert.assertThat(achieved, is(true));
	}

	@Test
	public void shouldNotTriggerIfEnoughLongCommentsButAllTheSame() {
		// GIVEN
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.process(new TimeTrackingItem("12345", LocalDateTime.now()));
		sut.done();

		// WHEN
		boolean achieved = sut.isAchieved();

		// THEN
		Assert.assertThat(achieved, is(false));
	}
}
