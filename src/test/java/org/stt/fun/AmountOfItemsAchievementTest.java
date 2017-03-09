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
        final LocalDateTime dateTime = LocalDateTime.now();
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(false));
	}

	@Test
	public void shouldBeAchievedWithExactlyEnoughItems() {
		// GIVEN
        final LocalDateTime dateTime = LocalDateTime.now();
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(true));
	}

	@Test
	public void shouldBeAchievedWithMoreThanEnoughItems() {
		// GIVEN
        final LocalDateTime dateTime = LocalDateTime.now();
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.process(new TimeTrackingItem("", dateTime));
        sut.done();
		// WHEN
		boolean achieved = sut.isAchieved();
		// THEN
		Assert.assertThat(achieved, is(true));
	}
}
