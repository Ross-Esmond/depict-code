function AppointmentField() {
  const [appointment, setAppointment] = useState()
  // notice the need to convert the date
  const asDate = dateInputToDate(appointment)

  return <div>
    <DateField value={appointment} onChange={setAppointment} />
    <Error show={asDate.valueOf() <= Date.now()}>
      Date must be in the future.
    </Error>
    <button onClick={handleSubmit}>Set Appointment</button>
  </div>
}
