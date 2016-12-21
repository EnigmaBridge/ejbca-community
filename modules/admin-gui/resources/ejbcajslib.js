<!--
/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

/*
 * JavaScript library used by EJBCA JSP pages
 * Originally created by Philip Vendil 2002-03-27
 */

function checkAll(checkboxlist,size) {
  for (var i = 0; i < size; i++) {
    box = eval(checkboxlist + i  ); 
    if (box.checked == false)
      box.checked = true;
  }
}

function uncheckAll(checkboxlist,size) {
  for (var i = 0; i < size; i++) {
  box = eval(checkboxlist + i ); 
  if (box.checked == true) 
    box.checked = false;
  }
}

function switchAll(checkboxlist,size) {
  for (var i = 0; i < size; i++) {
    box = eval(checkboxlist+ i);
    box.checked = !box.checked;
  }
}

// Function that checks thru an array of checkboxes an return true if only one is checked,
// if several is checked the alerttext is displayed, if none is selected then only false is returned.
function onlyoneselected(checkboxlist,size,alerttext){
  var numberofchecked=0;
  for(var i = 0; i < size; i++){
    box = eval(checkboxlist + i);   
    if (box.checked == true){
      numberofchecked++;
    }  
  }

  if(numberofchecked > 1){
    alert(alerttext);
  }

  return (numberofchecked==1);
}

function displayHelpWindow(helplink) {
  window.open(helplink, 'ejbca_helpwindow','height=600,width=500,scrollbars=yes,toolbar=yes,resizable=1');
}

function checkfieldforlegalchars(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9@\.\*\,\-:\/\?\'\=\(\)\|.]/g; 

  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalcharswithchangeable(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9@\.\-;]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegaldnchars(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9@\.\&\*\,\\\-:\/\?\'\=\#\(\)\|]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforcompletednchars(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9@\,\=\.\*\-:\/\?\'\#\(\)\|\\]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegaldncharswithchangeable(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9@\.\,\*\-:\/;\'\?\+\=\#\(\)\|\"]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforipaddess(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  // 0-9, A-F, a-f, ., :
  re = /[^\u0030-\u0039\u0041-\u0046\u0061-\u0066\.\:]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalemailchars(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_0-9@\.\-\']/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalemailcharswithchangeable(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_0-9@\.\-;]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalemailcharswithoutat(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_0-9\.\-\']/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalemailcharswithoutatwithchangeable(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_0-9\.\-;]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}


function checkfieldfordecimalnumbers(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^0-9]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforgender(thetextfield, alerttext) {
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^MmFf]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;  
  }
  else{
  	return true;
  }	
  
}

/** Verify that the field is of format '*d *h *m' */
function checkFieldForSimpleTimeMinutes(thetextfield , alerttext) {
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /^\s*(\d+\s*[dD])?\s*(\d+\s*[hH])?\s*(\d+\s*[mM])?\s*$/;
  if (re.exec(text)) {
	  return true;
  }
  alert(alerttext);
  return false;
}

/** Verify that the field is of format '*y *mo *d' or decimal */
function checkFieldForYearsMonthsDays(thetextfield , alerttext) {
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /^\s*((\d+\s*[yY])?\s*(\d+\s*[mM][oO])?\s*(\d+\s*[dD])?)\s*$|^\s*[0-9]+\s*$/;
  if (re.exec(text)) {
	  return true;
  }
  alert(alerttext);
  return false;
}

/** Verify that the field is of format '*y *mo *d *h *m' */
function checkFieldForCombineTime(thetextfield, alerttext) {
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /^\s*(\d+\s*[yY])?\s*(\d+\s*[mM][oO])?\s*(\d+\s*[dD])?\s*(\d+\s*[hH])?\s*(\d+\s*[mM])?\s*$/;
  if (re.exec(text)) {
	  return true;
  }
  alert(alerttext);
  return false;
}

/** Verify that the field is of format 'YYYYMMDD' */
function checkFieldForDate(thetextfield, alerttext) {
	field = eval(thetextfield);
	var text = new String(field.value);
	re = /(19|20)[0-9]{2}(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])/;
	if ( (text.length > 0) && ((!re.exec(text) || (text.length != 8))) ) {
		alert(alerttext);
		return false;
	}
	else {
		return true;
	}
}

function checkfieldforhexadecimalnumbers(thetextfield , alerttext){
  // remove all spaces
  field = eval(thetextfield);
  var text = new String(field.value);  
  re = /[^a-fA-F0-9 ]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function checkfieldforlegalresourcechars(thetextfield , alerttext){
  field = eval(thetextfield);
  var text = new String(field.value);
  re = /[^\u0041-\u005a\u0061-\u007a\u00a1-\ud7ff\ue000-\uffff_ 0-9\/]/g;
  if(re.exec(text)){
    alert(alerttext);
    return false;
  }
  else{
    return true;
  }
}

function trim(s) {
  while (s.substring(0,1) == ' ') {
    s = s.substring(1,s.length);
  }
  while (s.substring(s.length-1,s.length) == ' ') {
    s = s.substring(0,s.length-1);
  }
  return s;
}

// Prompt user for input, validate it for illegal chars and write it to a field
function getInputToField(fieldname, infotext, infovalidchars) {
	var input = prompt(infotext,"");
	if ( input != null && "" != input) {
		document.getElementById(fieldname).value = input;
		if (checkfieldforlegalchars(document.getElementById(fieldname), infovalidchars)) {
			return true;
		}
		document.getElementById(fieldname).value = '';
	}
	return false;
}

// Validate and write the 'input' to a field
function getInsertIntoField(fieldname, input, infovalidchars) {
	if ( input != null && "" != input) {
		document.getElementById(fieldname).value = input;
		if (checkfieldforlegalchars(document.getElementById(fieldname), infovalidchars)) {
			return true;
		}
	}
	return false;
}
-->