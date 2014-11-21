/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.stt.fun;

import static org.hamcrest.CoreMatchers.is;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;

/**
 *
 * @author dante
 */
public class AmountOfItemsAchievementTest extends AchievementTestBase {

	private AmountOfItemsAchievement sut;

	@Before
	public void setup() {
		sut = new AmountOfItemsAchievement(resourceBundle, 3);
		sut.start();
	}

	@Test
	public void shouldNotBeAchievedWithTooFewItems() {
		// GIVEN
		final DateTime dateTime = DateTime.now();
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(false));
	}

	@Test
	public void shouldBeAchievedWithExactlyEnoughItems() {
		// GIVEN
		final DateTime dateTime = DateTime.now();
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(true));
	}

	@Test
	public void shouldBeAchievedWithMoreThanEnoughItems() {
		// GIVEN
		final DateTime dateTime = DateTime.now();
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.process(new TimeTrackingItem(null, dateTime));
		sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(true));
	}
}
