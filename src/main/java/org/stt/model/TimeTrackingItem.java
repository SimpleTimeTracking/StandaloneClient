package org.stt.model;


public class TimeTrackingItem
{

   private String comment;
   

   public TimeTrackingItem()
   {

   }
   
   public TimeTrackingItem(String comment)
   {
      this.comment = comment;
   }

   public String getComment()
   {
      return comment;
   }

   public void setComment(String comment)
   {
      this.comment = comment;
   }

}
